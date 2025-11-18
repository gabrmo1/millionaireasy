package br.com.bot_mexc.services;

import br.com.bot_mexc.configs.RabbitMQConfig;
import br.com.bot_mexc.models.dtos.mexc.MexcKlineEventDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketMessageHandlerService {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Async("asyncExecutor")
    public void handleKlineMessage(String klineDataJson, String symbol, String interval) {
        final var threadName = Thread.currentThread().getName();

        try {
            final var wsRequest = objectMapper.readValue(klineDataJson, MexcKlineEventDTO.class);
            final var eventoKlineWS = new MexcKlineEventDTO(
                    symbol,
                    interval,
                    wsRequest.windowStart(),
                    wsRequest.windowEnd(),
                    wsRequest.open(),
                    wsRequest.close(),
                    wsRequest.high(),
                    wsRequest.low(),
                    wsRequest.volume(),
                    wsRequest.amount()
            );

            final var jsonParaFila = objectMapper.writeValueAsString(eventoKlineWS);
            final var routingKey = String.format("%s.%s.%s", RabbitMQConfig.KLINE_ROUTING_KEY_PREFIX, symbol, interval);

            rabbitTemplate.convertAndSend(RabbitMQConfig.MEXC_DATA_TOPIC, routingKey, jsonParaFila);
        } catch (Exception e) {
            log.error("+++ [ASYNC_HANDLER] Falha ao processar/publicar K-line (Thread: {}): {}", threadName, e.getMessage(), e);
        }
    }
}