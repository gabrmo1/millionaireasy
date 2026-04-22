package br.com.bot_mexc.consumers;

import br.com.bot_mexc.builders.AnaliseBuilder;
import br.com.bot_mexc.configs.RabbitMQConfig;
import br.com.bot_mexc.models.dtos.CandleDTO;
import br.com.bot_mexc.models.dtos.ContextoAnaliseDTO;
import br.com.bot_mexc.models.dtos.OperacaoCacheDTO;
import br.com.bot_mexc.models.dtos.mexc.EventoCandleMexcDTO;
import br.com.bot_mexc.models.entities.IndicadorConfig;
import br.com.bot_mexc.services.*;
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
    private final GestaoOrdemService gestaoOrdemService;
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
                    gestaoOrdemService.registrarIntencaoDeVenda(operacao.id(), operacao.par(), operacao.intervalo(), candle.valorFechamento(), indicadores, dataAberturaCandle);
                }
            } else {
                boolean sinalCompra = avaliacaoCondicaoService.avaliarCondicoesCompra(operacao.condicoesCompra(), indicadores);
                if (sinalCompra) {
                    log.info("Sinal de COMPRA detectado para Operação ID: {}", operacao.id());
                    gestaoOrdemService.registrarIntencaoDeCompra(operacao.id(), operacao.par(), operacao.intervalo(), candle.valorFechamento(), indicadores, dataAberturaCandle);
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