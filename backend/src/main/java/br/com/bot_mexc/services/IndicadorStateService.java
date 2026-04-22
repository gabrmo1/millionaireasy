package br.com.bot_mexc.services;

import br.com.bot_mexc.models.dtos.CandleDTO;
import br.com.bot_mexc.models.dtos.EstadoIndicadoresDTO;
import br.com.bot_mexc.models.dtos.IndicadorConfigDTO;
import br.com.bot_mexc.models.entities.IndicadorConfig;
import br.com.bot_mexc.models.enums.TipoIndicador;
import br.com.bot_mexc.repositories.CandleRepository;
import br.com.bot_mexc.utils.CalculoUtils;
import br.com.bot_mexc.utils.CandleUtils;
import br.com.bot_mexc.utils.constants.IndicadorKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndicadorStateService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CandleRepository candleRepository;
    private final MexcConnectionService mexcConnectionService;
    private final ObjectMapper objectMapper;

    private static final String STATE_KEY_PREFIX = "mexc:indicador:state:";
    private static final String HISTORY_KEY_PREFIX = "mexc:price:history:";

    /**
     * Força a inicialização do estado dos indicadores baseada no histórico.
     * Deve ser chamado ao iniciar uma operação para evitar "previous" nulo.
     */
    public void warmupState(String par, String intervalo, Set<IndicadorConfig> configs) {
        log.info("Realizando warmup de indicadores para {}/{}", par, intervalo);
        initializeStateFromHistory(par, intervalo, configs);
    }

    public EstadoIndicadoresDTO getOrInitializeState(String par, String intervalo, Set<IndicadorConfig> configs, long currentCandleTime) {
        final var stateKey = getStateKey(par, intervalo);
        EstadoIndicadoresDTO state = null;

        try {
            var cached = redisTemplate.opsForValue().get(stateKey);
            if (cached != null) {
                state = objectMapper.convertValue(cached, EstadoIndicadoresDTO.class);
            }
        } catch (Exception e) {
            log.warn("Erro ao ler estado do Redis: {}", e.getMessage());
        }

        if (state == null || isStateStale(state, currentCandleTime, intervalo)) {
            log.info("Estado inválido/ausente para {}/{}. Inicializando via histórico...", par, intervalo);
            return initializeStateFromHistory(par, intervalo, configs);
        }

        return state;
    }

    public void advanceState(String par, String intervalo, Set<IndicadorConfig> configs, CandleDTO closedCandle) {
        final var currentState = getOrInitializeState(par, intervalo, configs, closedCandle.dataFechamento());

        if (currentState.ultimaDataFechamento().equals(closedCandle.dataFechamento()))
            return;

        final var novosEstados = new HashMap<>(currentState.estados());
        updatePriceHistory(par, intervalo, closedCandle.valorFechamento());

        var chavesProcessadasNestaRodada = new HashSet<String>();

        for (IndicadorConfig config : configs) {
            final var key = generateCanonicalKey(config);

            if (chavesProcessadasNestaRodada.contains(key))
                continue;

            final var params = IndicadorConfigDTO.parametrosFromJson(config.getParametros());
            final var estadoAnt = currentState.estados().get(key);

            if (estadoAnt == null)
                continue;

            EstadoIndicadoresDTO.EstadoIndicadorItem novoItem = null;

            try {
                switch (config.getTipoIndicador()) {
                    case EMA:
                        final var pEma = params.getOrDefault(IndicadorKeys.PARAM_PERIODO_EMA, 200);
                        final var novaEma = CalculoUtils.calcularEmaIncremental(closedCandle.valorFechamento(), estadoAnt.valor(), pEma);
                        novoItem = new EstadoIndicadoresDTO.EstadoIndicadorItem(novaEma, null, null, null);
                        break;
                    case RSI_CURTO:
                    case RSI_MEDIO:
                    case RSI_LONGO:
                        final var pRsi = getPeriodoRsi(config, params);
                        final var variacao = closedCandle.valorFechamento().subtract(currentState.ultimoPrecoFechamento());
                        final var ganho = variacao.compareTo(BigDecimal.ZERO) > 0 ? variacao : BigDecimal.ZERO;
                        final var perda = variacao.compareTo(BigDecimal.ZERO) < 0 ? variacao.abs() : BigDecimal.ZERO;

                        final var novaMediaGanho = CalculoUtils.calcularMediaGanhoRsi(estadoAnt.avgGain(), pRsi, ganho);
                        final var novaMediaPerda = CalculoUtils.calcularMediaPerdaRsi(estadoAnt.avgLoss(), pRsi, perda);
                        final var novoRsi = CalculoUtils.calculateRsiFromAverages(novaMediaGanho, novaMediaPerda);

                        novoItem = new EstadoIndicadoresDTO.EstadoIndicadorItem(novoRsi, novaMediaGanho, novaMediaPerda, null);
                        break;
                }
            } catch (Exception e) {
                log.error("Erro ao avançar estado incremental para {}: {}", key, e.getMessage());
            }

            if (novoItem != null) {
                novosEstados.put(key, novoItem);
                chavesProcessadasNestaRodada.add(key);
            }
        }

        final var newState = new EstadoIndicadoresDTO(
                closedCandle.dataFechamento(),
                closedCandle.valorFechamento(),
                novosEstados
        );

        redisTemplate.opsForValue().set(getStateKey(par, intervalo), newState, 7, TimeUnit.DAYS);
    }

    private void updatePriceHistory(String par, String intervalo, BigDecimal price) {
        final var key = HISTORY_KEY_PREFIX + par + ":" + intervalo;
        redisTemplate.opsForList().rightPush(key, price);
        redisTemplate.opsForList().trim(key, -300, -1);
    }

    private EstadoIndicadoresDTO initializeStateFromHistory(String par, String intervalo, Set<IndicadorConfig> configs) {
        final var history = fetchCandlesHistory(par, intervalo);

        if (history.isEmpty())
            return null;

        final var lastCandle = history.getLast();
        final var estados = new HashMap<String, EstadoIndicadoresDTO.EstadoIndicadorItem>();

        final var historyKey = HISTORY_KEY_PREFIX + par + ":" + intervalo;
        redisTemplate.delete(historyKey);

        final var prices = history.stream().map(CandleDTO::valorFechamento).toList();
        redisTemplate.opsForList().rightPushAll(historyKey, prices.toArray());

        for (IndicadorConfig config : configs) {
            final var key = generateCanonicalKey(config);

            if (estados.containsKey(key)) continue;

            var params = IndicadorConfigDTO.parametrosFromJson(config.getParametros());

            if (config.getTipoIndicador() == TipoIndicador.EMA) {
                final var p = params.getOrDefault(IndicadorKeys.PARAM_PERIODO_EMA, 200);
                final var val = CalculoUtils.calcularEma(prices, p);
                estados.put(key, new EstadoIndicadoresDTO.EstadoIndicadorItem(val, null, null, null));
            } else if (isRsi(config.getTipoIndicador())) {
                final var p = getPeriodoRsi(config, params);

                final var triplo = CalculoUtils.calcularTriploRsi(history, p, p, p);

                BigDecimal rsiVal = config.getTipoIndicador() == TipoIndicador.RSI_CURTO ? triplo.rsiCurto() :
                        config.getTipoIndicador() == TipoIndicador.RSI_MEDIO ? triplo.rsiMedio() : triplo.rsiLongo();

                estados.put(key, new EstadoIndicadoresDTO.EstadoIndicadorItem(
                        rsiVal,
                        triplo.mediaGanhoFinal(),
                        triplo.mediaPerdaFinal(),
                        null
                ));
            }
        }

        final var state = new EstadoIndicadoresDTO(
                lastCandle.dataFechamento(),
                lastCandle.valorFechamento(),
                estados
        );

        redisTemplate.opsForValue().set(getStateKey(par, intervalo), state, 7, TimeUnit.DAYS);
        return state;
    }

    private List<CandleDTO> fetchCandlesHistory(String par, String intervalo) {
        final var dbCandles = candleRepository.findTopCandlesDesc(par, intervalo, org.springframework.data.domain.PageRequest.of(0, 300));
        if (dbCandles.size() >= 300) {
            dbCandles.sort(Comparator.comparing(br.com.bot_mexc.models.entities.Candle::getDataFechamento));
            return dbCandles.stream().map(CandleUtils::buildDtoFromEntity).toList();
        }
        return mexcConnectionService.consultarCandles(par, intervalo, String.valueOf(300));
    }

    private boolean isStateStale(EstadoIndicadoresDTO state, long currentCandleTime, String intervalo) {
        final var instantTempoAtualCandle = Instant.ofEpochSecond(currentCandleTime);
        final var instantUltimaDataFechamento = Instant.ofEpochSecond(state.ultimaDataFechamento());
        final var minutesDiff = Duration.between(instantUltimaDataFechamento, instantTempoAtualCandle).toMinutes();
        final var intervalMinutes = parseInterval(intervalo);

        return minutesDiff > (intervalMinutes * 2);
    }

    private long parseInterval(String interval) {
        if (interval.endsWith("m")) return Long.parseLong(interval.replace("m", ""));
        if (interval.endsWith("h")) return Long.parseLong(interval.replace("h", "")) * 60;
        if (interval.endsWith("d")) return Long.parseLong(interval.replace("d", "")) * 1440;
        return 60;
    }

    public String generateCanonicalKey(IndicadorConfig config) {
        final var params = IndicadorConfigDTO.parametrosFromJson(config.getParametros());
        final var sortedParams = new TreeMap<>(params);
        final var sb = new StringBuilder();

        sb.append(config.getTipoIndicador().name());

        if (!sortedParams.isEmpty()) {
            sb.append("_");
            sortedParams.forEach((k, v) -> sb.append(k).append("=").append(v).append("|"));
        }

        return sb.toString();
    }

    private String getStateKey(String par, String intervalo) {
        return STATE_KEY_PREFIX + par + ":" + intervalo;
    }

    private boolean isRsi(TipoIndicador t) {
        return t == TipoIndicador.RSI_CURTO || t == TipoIndicador.RSI_MEDIO || t == TipoIndicador.RSI_LONGO;
    }

    private int getPeriodoRsi(IndicadorConfig c, Map<String, Integer> p) {
        if (c.getTipoIndicador() == TipoIndicador.RSI_CURTO)
            return p.getOrDefault(IndicadorKeys.PARAM_PERIODO_RSI_CURTO, 7);

        if (c.getTipoIndicador() == TipoIndicador.RSI_MEDIO)
            return p.getOrDefault(IndicadorKeys.PARAM_PERIODO_RSI_MEDIO, 14);

        return p.getOrDefault(IndicadorKeys.PARAM_PERIODO_RSI_LONGO, 21);
    }
}