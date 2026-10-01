package br.com.bot_mexc.modules.strategy.consumers;
import br.com.bot_mexc.shared.enums.*;
import br.com.bot_mexc.shared.utils.DateUtils;
import br.com.bot_mexc.shared.constants.IndicadorKeys;
import br.com.bot_mexc.shared.configs.RabbitMQConfig;
import br.com.bot_mexc.shared.configs.RedisConfig;
import br.com.bot_mexc.modules.strategy.services.*;
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
import br.com.bot_mexc.modules.timeseries.services.CandleService;
import br.com.bot_mexc.modules.timeseries.services.AnaliseService;
import br.com.bot_mexc.modules.timeseries.repositories.AnaliseRepository;
import br.com.bot_mexc.modules.timeseries.builders.AnaliseBuilder;
import br.com.bot_mexc.shared.events.TradeSignalEvent;
import org.springframework.context.ApplicationEventPublisher;
import java.time.Instant;

import br.com.bot_mexc.modules.timeseries.builders.AnaliseBuilder;
import br.com.bot_mexc.shared.configs.RabbitMQConfig;
import br.com.bot_mexc.modules.timeseries.dtos.CandleDTO;
import br.com.bot_mexc.modules.strategy.dtos.ContextoAnaliseDTO;
import br.com.bot_mexc.modules.strategy.dtos.OperacaoCacheDTO;
import br.com.bot_mexc.shared.dtos.mexc.EventoCandleMexcDTO;
import br.com.bot_mexc.modules.strategy.entities.IndicadorConfig;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KlineAnalysisConsumer {

    private final AvaliacaoCondicaoService avaliacaoCondicaoService;
    private final CalculoIndicadorService calculoIndicadorService;
    private final IndicadorStateService indicadorStateService;
    private final OperacaoCacheService operacaoCacheService;
    private final ApplicationEventPublisher eventPublisher;
    private final CandleService candleService;
    private final AnaliseService analiseService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PREFIXO_THROTTLE = "mexc:analysis:throttle:";

    @RabbitListener(queues = RabbitMQConfig.KLINE_ANALYSIS_QUEUE)
    public void consumirMensagemKline(EventoCandleMexcDTO evento) {
        try {
            final var contexto = montarContextoDeAnalise(evento);

            if (contexto.semOperacoes()) {
                pausarProcessamentoParaParInativo(evento.par(), evento.intervalo());
                return;
            }

            var configsIndicadores = contexto.extrairConfiguracoesUnicasDeIndicadores();
            var indicadores = calculoIndicadorService.calcularIndicadoresOtimizado(
                    contexto.getPar(),
                    contexto.getIntervalo(),
                    contexto.candle(),
                    configsIndicadores
            );

            if (contexto.isFechamentoDeCandle()) {
                processarFechamentoDeCandle(contexto, indicadores);
            }
            // TODO: Reativar futuramente com config de realizar operações Intra-candle (antes do fechamento)
            // else {
            //    processarMovimentacaoDePreco(contexto, indicadores);
            // }

        } catch (Exception e) {
            log.error("Erro crítico no fluxo de análise de Kline: {}", e.getMessage(), e);
        }
    }

    private ContextoAnaliseDTO montarContextoDeAnalise(EventoCandleMexcDTO evento) {
        var candleRecebido = converterEventoParaCandle(evento);
        var operacoes = operacaoCacheService.getOperacoesAtivas(evento.par(), traduzirIntervaloParaTrading(evento.intervalo()));
        var candleFechadoOpt = candleService.verificarViradaEAtualizarCache(candleRecebido, evento.par(), evento.intervalo());

        return candleFechadoOpt.map(candleDTO ->
                        new ContextoAnaliseDTO(candleDTO, operacoes, true))
                .orElseGet(() -> new ContextoAnaliseDTO(candleRecebido, operacoes, false));
    }

    private void processarFechamentoDeCandle(ContextoAnaliseDTO contexto, Map<String, BigDecimal> indicadores) {
        log.info("Virada de candle detectada para {} (Fechamento: {}).", contexto.getPar(), contexto.candle().valorFechamento());

        eventPublisher.publishEvent(new br.com.bot_mexc.shared.events.MarketDataUpdateEvent(
                contexto.getPar(),
                contexto.getIntervalo(),
                contexto.candle().valorAbertura(),
                contexto.candle().valorFechamento(),
                contexto.candle().minima(),
                contexto.candle().maxima(),
                contexto.candle().volume(),
                contexto.candle().dataAbertura(),
                contexto.candle().dataFechamento(),
                true
        ));

        avaliarEstrategiasEExecutarOrdens(contexto.operacoes(), indicadores, contexto.candle());
        salvarAnalisesParaMonitoramento(contexto, indicadores);

        var configsIndicadores = contexto.extrairConfiguracoesUnicasDeIndicadores();
        indicadorStateService.advanceState(contexto.getPar(), contexto.getIntervalo(), configsIndicadores, contexto.candle());
    }

    //TODO: Verificar por quê o horário do salvamento está diferente do horário do candle
    private void salvarAnalisesParaMonitoramento(ContextoAnaliseDTO contexto, Map<String, BigDecimal> indicadoresCalculados) {
        var assinaturasProcessadas = new HashSet<>();

        for (OperacaoCacheDTO operacao : contexto.operacoes()) {
            final var assinatura = operacao.indicadores().stream()
                    .map(IndicadorConfig::getId)
                    .sorted()
                    .collect(Collectors.joining(","));

            if (assinaturasProcessadas.contains(assinatura)) {
                continue;
            }

            try {
                final var analise = AnaliseBuilder.montarAnalise(
                        contexto.getPar(),
                        contexto.getIntervalo(),
                        contexto.candle(),
                        operacao.indicadores(),
                        indicadoresCalculados
                );
                analiseService.salvarAnaliseAsync(analise);
                assinaturasProcessadas.add(assinatura);
            } catch (Exception e) {
                log.error("Erro ao salvar análise para operação {}: {}", operacao.id(), e.getMessage());
            }
        }
    }

    //TODO: NÃO REMOVER
//    private void processarMovimentacaoDePreco(ContextoAnaliseDTO contexto, Map<String, BigDecimal> indicadores) {
//        if (estaEmPeriodoDeThrottle(contexto.getPar(), contexto.getIntervalo()))
//            return;
//
//        avaliarEstrategiasEExecutarOrdens(contexto.operacoes(), indicadores, contexto.candle());
//    }

    private void avaliarEstrategiasEExecutarOrdens(List<OperacaoCacheDTO> operacoes, Map<String, BigDecimal> indicadores, CandleDTO candle) {
        final var dataAberturaCandle = candle.dataAbertura();

        operacoes.forEach(operacao -> {
            if (Boolean.TRUE.equals(operacao.posicionado())) {
                boolean sinalVenda = avaliacaoCondicaoService.avaliarCondicoesVenda(operacao.condicoesVenda(), indicadores);
                if (sinalVenda) {
                    log.info("Sinal de VENDA detectado para Operação ID: {}", operacao.id());
                    eventPublisher.publishEvent(new TradeSignalEvent(
                            operacao.id(),
                            operacao.par(),
                            operacao.intervalo(),
                            TradeSignalEvent.TipoSinal.VENDA,
                            candle.valorFechamento(),
                            indicadores,
                            dataAberturaCandle,
                            Instant.now()
                    ));
                }
            } else {
                boolean sinalCompra = avaliacaoCondicaoService.avaliarCondicoesCompra(operacao.condicoesCompra(), indicadores);
                if (sinalCompra) {
                    log.info("Sinal de COMPRA detectado para Operação ID: {}", operacao.id());
                    eventPublisher.publishEvent(new TradeSignalEvent(
                            operacao.id(),
                            operacao.par(),
                            operacao.intervalo(),
                            TradeSignalEvent.TipoSinal.COMPRA,
                            candle.valorFechamento(),
                            indicadores,
                            dataAberturaCandle,
                            Instant.now()
                    ));
                }
            }
        });
    }

    private CandleDTO converterEventoParaCandle(EventoCandleMexcDTO evento) {
        return new CandleDTO(
                evento.inicioJanela(),
                evento.fimJanela(),
                evento.precoAbertura(),
                evento.precoFechamento(),
                evento.minima(),
                evento.maxima(),
                evento.volume()
        );
    }

    private void pausarProcessamentoParaParInativo(String par, String intervalo) {
        var chave = PREFIXO_THROTTLE + par + ":" + intervalo;
        redisTemplate.opsForValue().set(chave, "PAUSED", 30, TimeUnit.SECONDS);
    }

    private String traduzirIntervaloParaTrading(String intervaloInterno) {
        if (intervaloInterno == null || intervaloInterno.isBlank()) {
            throw new ValidationException("O intervalo não pode ser nulo ou vazio.");
        }

        return switch (intervaloInterno) {
            case "Min1" -> "1m";
            case "Min5" -> "5m";
            case "Min15" -> "15m";
            case "Min30" -> "30m";
            case "Min60" -> "1h";
            case "Hour4" -> "4h";
            case "Hour8" -> "8h";
            case "Day1" -> "1d";
            case "Week1" -> "1w";
            default -> throw new ValidationException("Intervalo '" + intervaloInterno + "' não é suportado.");
        };
    }
}