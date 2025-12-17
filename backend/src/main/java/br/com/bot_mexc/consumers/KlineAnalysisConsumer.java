package br.com.bot_mexc.consumers;

import br.com.bot_mexc.configs.RabbitMQConfig;
import br.com.bot_mexc.models.dtos.CandleDTO;
import br.com.bot_mexc.models.dtos.ContextoAnaliseDTO;
import br.com.bot_mexc.models.dtos.OperacaoCacheDTO;
import br.com.bot_mexc.models.dtos.mexc.EventoCandleMexcDTO;
import br.com.bot_mexc.services.*;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    private final RedisTemplate<String, Object> redisTemplate;

    private static final ZoneId ZONA_BRASIL = ZoneId.of("America/Sao_Paulo");
    private static final String PREFIXO_THROTTLE = "mexc:analysis:throttle:";
    private static final long TEMPO_THROTTLE_SEGUNDOS = 3;

    @RabbitListener(queues = RabbitMQConfig.KLINE_ANALYSIS_QUEUE)
    public void consumirMensagemKline(EventoCandleMexcDTO evento) {
        try {
            final var contexto = montarContextoDeAnalise(evento);

            if (contexto.semOperacoes()) {
                pausarProcessamentoParaParInativo(evento.par(), evento.intervalo());
                return;
            }

            if (contexto.isFechamentoDeCandle()) {
                processarFechamentoDeCandle(contexto);
            } else {
                processarMovimentacaoDePreco(contexto);
            }

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

    private void processarFechamentoDeCandle(ContextoAnaliseDTO contexto) {
        log.info("Virada de candle detectada para {} (Fechamento: {}). Executando análise completa.",
                contexto.getPar(), contexto.candle().valorFechamento());

        var configsIndicadores = contexto.extrairConfiguracoesUnicasDeIndicadores();
        var indicadores = calculoIndicadorService.calcularIndicadoresOtimizado(
                contexto.getPar(),
                contexto.getIntervalo(),
                contexto.candle(),
                configsIndicadores
        );
        avaliarEstrategiasEExecutarOrdens(contexto.operacoes(), indicadores, contexto.candle());

        indicadorStateService.advanceState(contexto.getPar(), contexto.getIntervalo(), configsIndicadores, contexto.candle());
    }

    private void processarMovimentacaoDePreco(ContextoAnaliseDTO contexto) {
        if (estaEmPeriodoDeThrottle(contexto.getPar(), contexto.getIntervalo())) {
            return;
        }

        var configsIndicadores = contexto.extrairConfiguracoesUnicasDeIndicadores();
        var indicadores = calculoIndicadorService.calcularIndicadoresOtimizado(
                contexto.getPar(),
                contexto.getIntervalo(),
                contexto.candle(),
                configsIndicadores
        );

        avaliarEstrategiasEExecutarOrdens(contexto.operacoes(), indicadores, contexto.candle());
    }

    private void avaliarEstrategiasEExecutarOrdens(List<OperacaoCacheDTO> operacoes, Map<String, java.math.BigDecimal> indicadores, CandleDTO candle) {
        operacoes.forEach(operacao -> {
            boolean sinalCompra = avaliacaoCondicaoService.avaliarCondicoesCompra(operacao.condicoesCompra(), indicadores);

            if (sinalCompra) {
                gestaoOrdemService.registrarIntencaoDeCompra(operacao.id(), operacao.par(), operacao.intervalo(), candle.valorFechamento());
                return;
            }

            boolean sinalVenda = avaliacaoCondicaoService.avaliarCondicoesVenda(operacao.condicoesVenda(), indicadores);
            if (sinalVenda) {
                gestaoOrdemService.registrarIntencaoDeVenda(operacao.id(), operacao.par(), operacao.intervalo(), candle.valorFechamento());
            }
        });
    }

    private CandleDTO converterEventoParaCandle(EventoCandleMexcDTO evento) {
        return new CandleDTO(
                Instant.ofEpochSecond(evento.inicioJanela()).atZone(ZONA_BRASIL).toLocalDateTime(),
                Instant.ofEpochSecond(evento.fimJanela()).atZone(ZONA_BRASIL).toLocalDateTime(),
                evento.precoAbertura(),
                evento.precoFechamento(),
                evento.minima(),
                evento.maxima(),
                evento.volume()
        );
    }

    private boolean estaEmPeriodoDeThrottle(String par, String intervalo) {
        var chave = PREFIXO_THROTTLE + par + ":" + intervalo;
        var bloqueioAdquirido = redisTemplate.opsForValue().setIfAbsent(chave, "1", TEMPO_THROTTLE_SEGUNDOS, TimeUnit.SECONDS);
        return Boolean.FALSE.equals(bloqueioAdquirido);
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