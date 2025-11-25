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
        try {
            final var eventoKlineWS = objectMapper.readValue(klineDataJson, MexcKlineEventDTO.class);
            final var routingKey = String.format("%s.%s.%s", RabbitMQConfig.KLINE_ROUTING_KEY_PREFIX, symbol, interval);

            rabbitTemplate.convertAndSend(RabbitMQConfig.MEXC_DATA_TOPIC, routingKey, eventoKlineWS);
        } catch (Exception e) {
            log.error("+++ [ASYNC_HANDLER] Falha ao processar/publicar K-line: {}", e.getMessage(), e);
        }
    }
}