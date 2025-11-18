package br.com.bot_mexc.consumers;

import br.com.bot_mexc.configs.RabbitMQConfig;
import br.com.bot_mexc.models.dtos.CandleDTO;
import br.com.bot_mexc.models.dtos.mexc.MexcKlineEventDTO;
import br.com.bot_mexc.models.entities.Candle;
import br.com.bot_mexc.models.entities.Estrategia;
import br.com.bot_mexc.models.entities.Operacao;
import br.com.bot_mexc.models.enums.StatusOperacoes;
import br.com.bot_mexc.repositories.CandleRepository;
import br.com.bot_mexc.repositories.OperacaoRepository;
import br.com.bot_mexc.services.*;
import br.com.bot_mexc.utils.CandleUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KlineAnalysisConsumer {

    private final AvaliacaoCondicaoService avaliacaoCondicaoService;
    private final CalculoIndicadorService calculoIndicadorService;
    private final OperacaoRepository operacaoRepository;
    private final GestaoOrdemService gestaoOrdemService;
    private final CandleRepository candleRepository;
    private final CandleService candleService;
    private final ObjectMapper objectMapper;

    private static final ZoneId ZONA_BRASIL = ZoneId.of("America/Sao_Paulo");

    @RabbitListener(queues = RabbitMQConfig.KLINE_ANALYSIS_QUEUE)
    public void handleKlineMessage(String klineJson) {
        try {
            final var evento = objectMapper.readValue(klineJson, MexcKlineEventDTO.class);
            final var par = evento.symbol();
            final var intervalo = evento.interval();
            final var candleDto = converterEventoParaCandleDTO(evento);

            candleService.salvarCandleWebsocket(candleDto, par, intervalo);

            //TODO --- Inicio refatoração: Fazer com que as operações em andamento sejam salvas no redis e consultar dele
            List<Operacao> operacoesAtivas = operacaoRepository.findActiveOperationsEagerly(
                    StatusOperacoes.EM_ANDAMENTO, par, intervalo
            );

            if (operacoesAtivas.isEmpty()) {
                log.debug("Nenhuma operação ativa encontrada para {}/{}", par, intervalo);
                return;
            }
            //TODO --- fim refatoração:

            log.info("Iniciando análise de K-line para {}/{} ({} operações ativas)", par, intervalo, operacoesAtivas.size());

            //TODO --- Inicio refatoração: Caso não possua 200 candles salvos, consultar pela api da MEXC
            List<Candle> ultimos200Candles = candleRepository.findLast200CandlesAsc(par, intervalo);
            if (CollectionUtils.isEmpty(ultimos200Candles) || ultimos200Candles.size() < 50) {
                log.warn("Análise pulada. Pouco histórico de candles para {}/{} (Encontrados: {})", par, intervalo, ultimos200Candles.size());
                return;
            }
            List<CandleDTO> candlesDTOHistorico = ultimos200Candles.stream()
                    .map(CandleUtils::converterEntidadeParaDto)
                    .toList();
            candlesDTOHistorico.add(candleDto);
            //TODO --- fim refatoração

            for (Operacao operacao : operacoesAtivas) {
                Estrategia estrategia = operacao.getEstrategia();
                if (estrategia == null) continue;

                Map<String, BigDecimal> indicadores = calculoIndicadorService.calcularIndicadores(
                        candlesDTOHistorico,
                        estrategia.getIndicadoresConfig(),
                        candleDto.closeValue()
                );
                boolean deveComprar = avaliacaoCondicaoService.avaliarCondicoesCompra(estrategia.getCondicoesCompra(), indicadores);
                boolean deveVender = avaliacaoCondicaoService.avaliarCondicoesVenda(estrategia.getCondicoesVenda(), indicadores);

                if (deveComprar) {
                    gestaoOrdemService.registrarIntencaoDeCompra(operacao, candleDto.closeValue());
                } else if (deveVender) {
                    // TODO: Adicionar lógica de "Take Profit"
                    gestaoOrdemService.registrarIntencaoDeVenda(operacao, candleDto.closeValue());
                }
            }

        } catch (Exception e) {
            log.error("Falha ao processar K-line da fila. Mensagem será reenfileirada ou movida para DLQ. Erro: {}", e.getMessage(), e);
            throw new RuntimeException("Falha no processamento do K-line: " + e.getMessage(), e);
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