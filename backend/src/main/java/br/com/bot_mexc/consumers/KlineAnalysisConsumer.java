package br.com.bot_mexc.consumers;

import br.com.bot_mexc.configs.RabbitMQConfig;
import br.com.bot_mexc.models.dtos.CandleDTO;
import br.com.bot_mexc.models.dtos.OperacaoCacheDTO;
import br.com.bot_mexc.models.dtos.mexc.MexcKlineEventDTO;
import br.com.bot_mexc.models.entities.IndicadorConfig;
import br.com.bot_mexc.services.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class KlineAnalysisConsumer {

    private final AvaliacaoCondicaoService avaliacaoCondicaoService;
    private final CalculoIndicadorService calculoIndicadorService;
    private final IndicadorStateService indicadorStateService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OperacaoCacheService operacaoCacheService;
    private final GestaoOrdemService gestaoOrdemService;
    private final CandleService candleService;

    private static final ZoneId ZONA_BRASIL = ZoneId.of("America/Sao_Paulo");
    private static final long THROTTLE_SECONDS = 5;

    @RabbitListener(queues = RabbitMQConfig.KLINE_ANALYSIS_QUEUE)
    public void handleKlineMessage(MexcKlineEventDTO evento) {
        try {
            final var par = evento.symbol();
            final var intervalo = evento.interval();
            final var candleAtualDto = converterEventoParaCandleDTO(evento);

            final var candleFechadoOpt = candleService.processarCandleWebsocket(candleAtualDto, par, intervalo);
            final var isTurnover = candleFechadoOpt.isPresent();

            final var operacoesAtivas = operacaoCacheService.getOperacoesAtivas(par, intervalo);
            if (operacoesAtivas.isEmpty()) {
                expirarOperacoesDestePar(par, intervalo);
                return;
            }

            var todasConfigs = new HashSet<IndicadorConfig>();
            operacoesAtivas.forEach(op -> todasConfigs.addAll(op.indicadores()));

            if (isTurnover) {
                log.info("Virada de candle detectada para {}/{}. Processando fechamento e avançando estado.", par, intervalo);

                final var candleFechado = candleFechadoOpt.get();
                processarLogicaDeTrading(candleFechado, operacoesAtivas, todasConfigs);

                indicadorStateService.advanceState(par, intervalo, todasConfigs, candleFechado);
            } else {
                final var throttleKey = "mexc:analysis:throttle:" + par + ":" + intervalo;
                final var acquired = redisTemplate.opsForValue().setIfAbsent(throttleKey, "1", THROTTLE_SECONDS, TimeUnit.SECONDS);

                if (Boolean.FALSE.equals(acquired))
                    return;
            }

            processarLogicaDeTrading(candleAtualDto, operacoesAtivas, todasConfigs);

        } catch (Exception e) {
            log.error("Falha ao processar K-line da fila: {}", e.getMessage(), e);
        }
    }

    private void expirarOperacoesDestePar(String par, String intervalo) {
        redisTemplate.expire("mexc:analysis:throttle:" + par + ":" + intervalo, 30, TimeUnit.SECONDS);
    }

    private void processarLogicaDeTrading(CandleDTO candle, List<OperacaoCacheDTO> operacoes, Set<IndicadorConfig> configs) {
        final var indicadores = calculoIndicadorService.calcularIndicadoresOtimizado(
                operacoes.getFirst().par(),
                operacoes.getFirst().intervalo(),
                candle,
                configs
        );

        for (OperacaoCacheDTO operacaoDto : operacoes) {
            boolean deveComprar = avaliacaoCondicaoService.avaliarCondicoesCompra(operacaoDto.condicoesCompra(), indicadores);
            boolean deveVender = avaliacaoCondicaoService.avaliarCondicoesVenda(operacaoDto.condicoesVenda(), indicadores);

            if (deveComprar)
                gestaoOrdemService.registrarIntencaoDeCompra(operacaoDto.id(), operacaoDto.par(), candle.closeValue());
            else if (deveVender)
                gestaoOrdemService.registrarIntencaoDeVenda(operacaoDto.id(), operacaoDto.par(), candle.closeValue());

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