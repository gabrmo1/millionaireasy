package br.com.bot_mexc.services;

import br.com.bot_mexc.configs.RabbitMQConfig;
import br.com.bot_mexc.models.dtos.mexc.MexcKlineEventDTO;
import br.com.bot_mexc.proto.PublicSpotKlineV3Api;
import br.com.bot_mexc.proto.PushDataV3ApiWrapper;
import com.google.protobuf.InvalidProtocolBufferException;
import jakarta.websocket.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.ByteBuffer;

@Slf4j
@Component
public class MexcWebSocketClient {

    @Value("${mexc.websocket.url}")
    private String websocketUrl;

    private final RabbitTemplate rabbitTemplate;
    private final MexcSubscriptionService subscriptionService;
    private final WebSocketContainer container;

    private volatile boolean isConnecting = false;
    private int connectionAttempts = 0;
    private Session session;

    public MexcWebSocketClient(@Lazy MexcSubscriptionService subscriptionService,
                               RabbitTemplate rabbitTemplate,
                               @Value("${mexc.websocket.url}") String websocketUrl) {
        this.container = ContainerProvider.getWebSocketContainer();
        this.rabbitTemplate = rabbitTemplate;
        this.subscriptionService = subscriptionService;
        this.websocketUrl = websocketUrl;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationEvent() {
        log.info("Aplicação pronta. Iniciando conexão WebSocket...");
        this.connect();
    }

    @Async("asyncExecutor")
    public void connect() {
        if (isOpen() || isConnecting)
            return;

        isConnecting = true;
        connectionAttempts++;
        log.info("Tentando conectar ao WebSocket... (Tentativa {})", connectionAttempts);

        try {
            final var config = ClientEndpointConfig.Builder.create().build();
            final var endpointInstance = new Endpoint() {

                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    MexcWebSocketClient.this.session = session;
                    MexcWebSocketClient.this.isConnecting = false;
                    MexcWebSocketClient.this.connectionAttempts = 0;
                    log.info("WebSocket conectado: {}", session.getId());

                    session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
                        @Override
                        public void onMessage(ByteBuffer message) {
                            MexcWebSocketClient.this.handleBinaryMessage(message);
                        }
                    });

                    try {
                        subscriptionService.resyncSubscriptionsFromDatabase();
                    } catch (Exception e) {
                        log.error("Erro ao ressincronizar subscrições", e);
                    }
                }

                @Override
                public void onClose(Session session, CloseReason closeReason) {
                    MexcWebSocketClient.this.handleClose(closeReason);
                }

                @Override
                public void onError(Session session, Throwable throwable) {
                    MexcWebSocketClient.this.handleError(throwable);
                }
            };
            container.connectToServer(endpointInstance, config, URI.create(websocketUrl));
        } catch (Exception e) {
            log.error("Falha na conexão WS: {}", e.getMessage());
            isConnecting = false;
        }
    }

    private void handleBinaryMessage(ByteBuffer message) {
        try {
            final var bytes = new byte[message.remaining()];
            message.get(bytes);
            final var wrapper = PushDataV3ApiWrapper.parseFrom(bytes);

            if (!wrapper.hasPublicSpotKline()) return;

            final var klineData = wrapper.getPublicSpotKline();
            final var symbol = wrapper.getSymbol();
            final var interval = klineData.getInterval();
            final var dto = mapProtoToDto(klineData, symbol);

            final var routingKey = String.format("%s.%s.%s", RabbitMQConfig.KLINE_ROUTING_KEY_PREFIX, symbol, interval);

            rabbitTemplate.convertAndSend(RabbitMQConfig.MEXC_DATA_TOPIC, routingKey, dto, m -> {
                m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
                return m;
            });

        } catch (InvalidProtocolBufferException e) {
            log.error("Erro protobuf: {}", e.getMessage());
        }
    }

    private MexcKlineEventDTO mapProtoToDto(PublicSpotKlineV3Api proto, String symbol) {
        return new MexcKlineEventDTO(
                symbol,
                proto.getInterval(),
                proto.getWindowStart(),
                proto.getWindowEnd(),
                new BigDecimal(proto.getOpeningPrice()),
                new BigDecimal(proto.getClosingPrice()),
                new BigDecimal(proto.getHighestPrice()),
                new BigDecimal(proto.getLowestPrice()),
                new BigDecimal(proto.getVolume()),
                new BigDecimal(proto.getAmount())
        );
    }

    private void handleClose(CloseReason reason) {
        this.session = null;
        log.warn("WS Fechado. Reconectando...");
        connect();
    }

    private void handleError(Throwable t) {
        log.error("WS Error", t);
    }

    public void subscribe(String channel) {
        if (isOpen()) {
            try {
                session.getBasicRemote().sendText(String.format("{\"method\":\"SUBSCRIPTION\",\"params\":[\"%s\"]}", channel));
            } catch (IOException e) {
                log.error("Erro subscribe", e);
            }
        }
    }

    public void unsubscribe(String channel) {
        if (isOpen()) {
            try {
                session.getBasicRemote().sendText(String.format("{\"method\":\"UNSUBSCRIPTION\",\"params\":[\"%s\"]}", channel));
            } catch (IOException e) {
                log.error("Erro unsubscribe", e);
            }
        }
    }

    public boolean isOpen() {
        return this.session != null && this.session.isOpen();
    }

    @Scheduled(fixedRate = 20000)
    public void sendPing() {
        if (isOpen()) {
            try {
                session.getBasicRemote().sendText("{\"method\":\"PING\"}");
            } catch (IOException e) {
            }
        }
    }
}