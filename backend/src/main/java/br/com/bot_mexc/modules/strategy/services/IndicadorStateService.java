package br.com.bot_mexc.modules.strategy.services;
import br.com.bot_mexc.shared.enums.*;
import br.com.bot_mexc.shared.utils.DateUtils;
import br.com.bot_mexc.shared.constants.IndicadorKeys;
import br.com.bot_mexc.shared.configs.RabbitMQConfig;
import br.com.bot_mexc.shared.configs.RedisConfig;
import br.com.bot_mexc.modules.market.services.*;
import br.com.bot_mexc.modules.market.services.mexc.*;
import br.com.bot_mexc.modules.strategy.services.indicators.*;
import br.com.bot_mexc.modules.market.dtos.*;
import br.com.bot_mexc.modules.market.dtos.mexc.*;
import br.com.bot_mexc.modules.strategy.dtos.monitoramento.*;
import br.com.bot_mexc.modules.strategy.entities.CondicaoCompra;
import br.com.bot_mexc.modules.strategy.entities.CondicaoVenda;
import br.com.bot_mexc.modules.strategy.entities.IndicadorConfig;
import br.com.bot_mexc.modules.strategy.entities.Operacao;
import br.com.bot_mexc.modules.strategy.repositories.OperacaoRepository;
import br.com.bot_mexc.modules.strategy.repositories.CompraRepository;
import br.com.bot_mexc.modules.strategy.repositories.VendaRepository;
import br.com.bot_mexc.modules.timeseries.dtos.CandleDTO;
import br.com.bot_mexc.modules.timeseries.entities.Candle;
import br.com.bot_mexc.modules.timeseries.services.CandleService;
import br.com.bot_mexc.modules.timeseries.services.AnaliseService;
import br.com.bot_mexc.modules.timeseries.repositories.AnaliseRepository;
import br.com.bot_mexc.modules.timeseries.builders.AnaliseBuilder;


import br.com.bot_mexc.modules.timeseries.dtos.CandleDTO;
import br.com.bot_mexc.modules.strategy.dtos.EstadoIndicadoresDTO;
import br.com.bot_mexc.modules.strategy.dtos.IndicadorConfigDTO;
import br.com.bot_mexc.modules.strategy.dtos.PrimitiveCandle;
import br.com.bot_mexc.modules.strategy.entities.IndicadorConfig;
import br.com.bot_mexc.shared.enums.TipoIndicador;
import br.com.bot_mexc.modules.timeseries.repositories.CandleRepository;
import br.com.bot_mexc.modules.strategy.services.indicators.IndicadorContext;
import br.com.bot_mexc.modules.strategy.services.indicators.IndicadorState;
import br.com.bot_mexc.modules.strategy.services.indicators.IndicadorStrategy;
import br.com.bot_mexc.modules.strategy.services.indicators.IndicadorStrategyRegistry;
import br.com.bot_mexc.modules.timeseries.utils.CandleUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndicadorStateService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CandleRepository candleRepository;
    private final MexcConnectionService mexcConnectionService;
    private final ObjectMapper objectMapper;
    private final IndicadorStrategyRegistry strategyRegistry;

    private static final String STATE_KEY_PREFIX = "mexc:indicador:state:";
    private static final String HISTORY_KEY_PREFIX = "mexc:price:history:";

    // L1 Cache: Thread-safe, sem contenção de I/O, ideal para mutabilidade em alta frequência.
    private final Map<String, IndicadorState> l1Cache = new ConcurrentHashMap<>();

    public IndicadorState getL1State(String par, String intervalo, String canonicalKey, TipoIndicador tipo) {
        String cacheKey = par + ":" + intervalo + ":" + canonicalKey;
        return l1Cache.computeIfAbsent(cacheKey, k -> {
            var strategy = strategyRegistry.get(tipo);
            return strategy != null ? strategy.inicializarEstado() : null;
        });
    }

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

        PrimitiveCandle pCandle = new PrimitiveCandle(
                closedCandle.dataAbertura(), closedCandle.dataFechamento(),
                closedCandle.valorAbertura().doubleValue(), closedCandle.valorFechamento().doubleValue(),
                closedCandle.minima().doubleValue(), closedCandle.maxima().doubleValue(), closedCandle.volume().doubleValue()
        );

        double previousClose = currentState.ultimoPrecoFechamento() != null ? currentState.ultimoPrecoFechamento().doubleValue() : Double.NaN;
        var chavesProcessadas = new HashSet<String>();

        for (IndicadorConfig config : configs) {
            final var key = generateCanonicalKey(config);
            if (chavesProcessadas.contains(key)) continue;

            var strategy = strategyRegistry.get(config.getTipoIndicador());
            if (strategy != null) {
                var params = IndicadorConfigDTO.parametrosFromJson(config.getParametros());
                IndicadorState l1State = getL1State(par, intervalo, key, config.getTipoIndicador());
                IndicadorContext context = new IndicadorContext(params, l1State, previousClose, null);

                double calculatedValue = strategy.calcular(pCandle, context);

                // Grava L2 de forma segura para recuperação (Retrocompatível)
                novosEstados.put(key, EstadoIndicadoresDTO.EstadoIndicadorItem.builder()
                        .valor(Double.isNaN(calculatedValue) ? BigDecimal.ZERO : BigDecimal.valueOf(calculatedValue))
                        .build());
                chavesProcessadas.add(key);
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

    // =========================================================================================
    // TASK 2: Refatoração do Motor de Warmup Unificado e Zero-Allocation na iteração
    // =========================================================================================
    private EstadoIndicadoresDTO initializeStateFromHistory(String par, String intervalo, Set<IndicadorConfig> configs) {
        final var history = fetchCandlesHistory(par, intervalo);
        if (history.isEmpty()) return null;

        final var historyKey = HISTORY_KEY_PREFIX + par + ":" + intervalo;
        redisTemplate.delete(historyKey);
        final var prices = history.stream().map(CandleDTO::valorFechamento).toList();
        redisTemplate.opsForList().rightPushAll(historyKey, prices.toArray());

        // Arrays primitivos e referências diretas para evitar autoboxing/GC overhead no loop
        int numConfigs = configs.size();
        IndicadorStrategy[] strategyArray = new IndicadorStrategy[numConfigs];
        IndicadorContext[] contextArray = new IndicadorContext[numConfigs];
        String[] keysArray = new String[numConfigs];

        int idx = 0;
        for (IndicadorConfig config : configs) {
            String canonicalKey = generateCanonicalKey(config);
            var strategy = strategyRegistry.get(config.getTipoIndicador());

            if (strategy == null) {
                throw new IllegalStateException("Estratégia falhou/não encontrada no Registry: " + config.getTipoIndicador());
            }

            IndicadorState l1State = getL1State(par, intervalo, canonicalKey, config.getTipoIndicador());
            l1State.reset(); // Garante estado limpo antes do loop

            keysArray[idx] = canonicalKey;
            strategyArray[idx] = strategy;
            contextArray[idx] = new IndicadorContext(IndicadorConfigDTO.parametrosFromJson(config.getParametros()), l1State, Double.NaN, null);
            idx++;
        }

        double previousClose = Double.NaN;
        double[] finalValues = new double[numConfigs];

        // CPU-bound loop: Execução puramente vetorial nas estratégias
        for (CandleDTO dto : history) {
            PrimitiveCandle pCandle = new PrimitiveCandle(
                    dto.dataAbertura(), dto.dataFechamento(),
                    dto.valorAbertura().doubleValue(), dto.valorFechamento().doubleValue(),
                    dto.minima().doubleValue(), dto.maxima().doubleValue(), dto.volume().doubleValue()
            );

            for (int i = 0; i < numConfigs; i++) {
                contextArray[i] = new IndicadorContext(contextArray[i].parametros(), contextArray[i].estado(), previousClose, null);
                finalValues[i] = strategyArray[i].calcular(pCandle, contextArray[i]);
            }
            previousClose = pCandle.valorFechamento();
        }

        // Snapshot Final para o L2 (Redis)
        final var lastCandle = history.getLast();
        final var estadosL2 = new HashMap<String, EstadoIndicadoresDTO.EstadoIndicadorItem>();

        for (int i = 0; i < numConfigs; i++) {
            double val = finalValues[i];
            estadosL2.put(keysArray[i], EstadoIndicadoresDTO.EstadoIndicadorItem.builder()
                    .valor(Double.isNaN(val) ? BigDecimal.ZERO : BigDecimal.valueOf(val))
                    .build());
        }

        final var state = new EstadoIndicadoresDTO(
                lastCandle.dataFechamento(),
                lastCandle.valorFechamento(),
                estadosL2
        );

        redisTemplate.opsForValue().set(getStateKey(par, intervalo), state, 7, TimeUnit.DAYS);
        return state;
    }

    private List<CandleDTO> fetchCandlesHistory(String par, String intervalo) {
        final var dbCandles = candleRepository.findTopCandlesDesc(par, intervalo, org.springframework.data.domain.PageRequest.of(0, 300));
        if (dbCandles.size() >= 300) {
            // Ordenação asc garantida para simulação cronológica correta
            dbCandles.sort(Comparator.comparing(Candle::getDataFechamento));
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
}