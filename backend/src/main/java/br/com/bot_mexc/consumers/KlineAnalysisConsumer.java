package br.com.bot_mexc.consumers;

import br.com.bot_mexc.configs.RabbitMQConfig;
import br.com.bot_mexc.models.dtos.CandleDTO;
import br.com.bot_mexc.models.dtos.OperacaoCacheDTO;
import br.com.bot_mexc.models.dtos.mexc.MexcKlineEventDTO;
import br.com.bot_mexc.models.entities.Operacao;
import br.com.bot_mexc.repositories.CandleRepository;
import br.com.bot_mexc.repositories.OperacaoRepository;
import br.com.bot_mexc.services.*;
import br.com.bot_mexc.utils.CandleUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KlineAnalysisConsumer {

    private final AvaliacaoCondicaoService avaliacaoCondicaoService;
    private final CalculoIndicadorService calculoIndicadorService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OperacaoCacheService operacaoCacheService;
    private final OperacaoRepository operacaoRepository;
    private final GestaoOrdemService gestaoOrdemService;
    private final CandleRepository candleRepository;
    private final CandleService candleService;
    private final MexcService mexcService;

    private static final ZoneId ZONA_BRASIL = ZoneId.of("America/Sao_Paulo");
    private static final long THROTTLE_SECONDS = 5;

    @RabbitListener(queues = RabbitMQConfig.KLINE_ANALYSIS_QUEUE)
    public void handleKlineMessage(MexcKlineEventDTO evento) {
        try {
            final var par = evento.symbol();
            final var intervalo = evento.interval();
            final var candleDto = converterEventoParaCandleDTO(evento);

            candleService.salvarCandleWebsocket(candleDto, par, intervalo);

            String throttleKey = "mexc:analysis:throttle:" + par + ":" + intervalo;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(throttleKey)))
                return;

            List<OperacaoCacheDTO> operacoesAtivas = operacaoCacheService.getOperacoesAtivas(par, intervalo);

            if (operacoesAtivas.isEmpty()) {
                redisTemplate.opsForValue().set(throttleKey, "1", 30, TimeUnit.SECONDS);
                log.debug("Nenhuma operação ativa encontrada para {}/{}", par, intervalo);
                return;
            }

            redisTemplate.opsForValue().set(throttleKey, "1", THROTTLE_SECONDS, TimeUnit.SECONDS);

            var candlesParaAnalise = obterHistoricoCandles(par, intervalo, candleDto); //TODO: Remover carga do banco de dados: Pensar em uma solução inteligente
            candlesParaAnalise.removeIf(c -> c.closeTime().isEqual(candleDto.closeTime()));
            candlesParaAnalise.add(candleDto);

            log.info("Analisando {}/{} | Candles: {} | Operações: {}",
                    par, intervalo, candlesParaAnalise.size(), operacoesAtivas.size());

            for (OperacaoCacheDTO operacaoDto : operacoesAtivas) {
                analisarOperacao(operacaoDto, candlesParaAnalise, candleDto);
            }

        } catch (Exception e) {
            log.error("Falha ao processar K-line da fila: {}", e.getMessage(), e);
        }
    }

    private List<CandleDTO> obterHistoricoCandles(String par, String intervalo, CandleDTO candleAtual) {
        final var candlesSalvos = candleRepository.findLast200CandlesAsc(par, intervalo);
        boolean precisaBackfill = false;

        if (CollectionUtils.isEmpty(candlesSalvos) || candlesSalvos.size() < 199) {
            log.info("Histórico insuficiente no banco para {}/{}. Buscando snapshot na API...", par, intervalo);
            precisaBackfill = true;
        } else {
            // Verificação de Continuidade (Gap Detection)
            var ultimoSalvoEntity = candlesSalvos.get(candlesSalvos.size() - 1);
            var ultimoSalvoDto = CandleUtils.converterEntidadeParaDto(ultimoSalvoEntity);

            if (existeGapTemporal(ultimoSalvoDto, candleAtual, intervalo)) {
                log.warn("GAP detectado para {}/{}. Último DB: {}, Atual WS: {}. Forçando Backfill.",
                        par, intervalo, ultimoSalvoDto.closeTime(), candleAtual.openTime());
                precisaBackfill = true;
            }
        }

        if (precisaBackfill) {
            log.info("Executando Backfill (Snapshot API) para {}/{}...", par, intervalo);
            final var snapshotApi = mexcService.consultarCandles(par, intervalo, "200");

            // Salva o histórico recuperado para popular o banco
            candleService.salvarCandlesAsync(snapshotApi, par, intervalo);

            return new ArrayList<>(snapshotApi);
        }

        return candlesSalvos.stream()
                .map(CandleUtils::converterEntidadeParaDto)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private boolean existeGapTemporal(CandleDTO ultimoSalvo, CandleDTO candleAtual, String intervaloStr) {
        long minutosIntervalo = converterIntervaloParaMinutos(intervaloStr);
        if (minutosIntervalo == 0) return false;

        Duration diff = Duration.between(ultimoSalvo.closeTime(), candleAtual.openTime());
        long minutosDiff = Math.abs(diff.toMinutes());

        // Se a diferença for maior que (Intervalo * 1.5), consideramos gap.
        return minutosDiff > (minutosIntervalo * 1.5);
    }

    private long converterIntervaloParaMinutos(String intervalo) {
        if (intervalo.endsWith("m")) return Long.parseLong(intervalo.replace("m", ""));
        if (intervalo.endsWith("h")) return Long.parseLong(intervalo.replace("h", "")) * 60;
        if (intervalo.endsWith("d")) return Long.parseLong(intervalo.replace("d", "")) * 1440;
        return 0;
    }

    private void analisarOperacao(OperacaoCacheDTO operacaoDto, List<CandleDTO> historico, CandleDTO candleAtual) {
        var configsIndicadores = new HashSet<>(operacaoDto.indicadores());

        final var indicadores = calculoIndicadorService.calcularIndicadores(
                historico,
                configsIndicadores,
                candleAtual.closeValue()
        );

        // CORREÇÃO APLICADA: Envolvendo em new HashSet<>()
        boolean deveComprar = avaliacaoCondicaoService.avaliarCondicoesCompra(new HashSet<>(operacaoDto.condicoesCompra()), indicadores);
        boolean deveVender = avaliacaoCondicaoService.avaliarCondicoesVenda(new HashSet<>(operacaoDto.condicoesVenda()), indicadores);

        if (deveComprar || deveVender) {
            Operacao operacaoFull = operacaoRepository.findById(operacaoDto.id()).orElse(null);

            if (operacaoFull != null) {
                if (deveComprar) {
                    gestaoOrdemService.registrarIntencaoDeCompra(operacaoFull, candleAtual.closeValue());
                } else {
                    gestaoOrdemService.registrarIntencaoDeVenda(operacaoFull, candleAtual.closeValue());
                }
            } else {
                log.warn("Tentativa de operar em operação não encontrada no DB: {}", operacaoDto.id());
            }
        }
    }

    private CandleDTO converterEventoParaCandleDTO(MexcKlineEventDTO k) {
        return new CandleDTO(
                Instant.ofEpochSecond(k.windowStart()).atZone(ZONA_BRASIL).toLocalDateTime(),
                Instant.ofEpochSecond(k.windowEnd()).atZone(ZONA_BRASIL).toLocalDateTime(),
                k.open(),
                k.close(),
                k.low(),
                k.high(),
                k.volume()
        );
    }
}