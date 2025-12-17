package br.com.bot_mexc.services;

import br.com.bot_mexc.configs.RabbitMQConfig;
import br.com.bot_mexc.models.dtos.mexc.EventoCandleMexcDTO;
import br.com.bot_mexc.proto.PublicSpotKlineV3Api;
import br.com.bot_mexc.proto.PushDataV3ApiWrapper;
import com.google.protobuf.InvalidProtocolBufferException;
import jakarta.websocket.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class MexcWebSocketClient extends Endpoint {

    private final RabbitTemplate rabbitTemplate;
    private final String websocketUrl;
    private final WebSocketContainer container;

    @Getter
    private final String id;

    private Session session;
    private final AtomicInteger subscriptionCount = new AtomicInteger(0);
    private final Set<String> activeChannels = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final ConcurrentLinkedQueue<String> pendingSubscriptions = new ConcurrentLinkedQueue<>();

    public static final int MAX_SUBSCRIPTIONS = 20;

    public MexcWebSocketClient(String id, String websocketUrl, RabbitTemplate rabbitTemplate) {
        this.id = id;
        this.websocketUrl = websocketUrl;
        this.rabbitTemplate = rabbitTemplate;
        this.container = ContainerProvider.getWebSocketContainer();
        this.container.setDefaultMaxBinaryMessageBufferSize(1024 * 1024);
    }

    public void connect() {
        try {
            log.info("[WSClient-{}] Conectando a {}...", id, websocketUrl);
            ClientEndpointConfig config = ClientEndpointConfig.Builder.create().build();
            this.container.connectToServer(this, config, URI.create(websocketUrl));
        } catch (Exception e) {
            log.error("[WSClient-{}] Falha na conexão: {}", id, e.getMessage());
        }
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        this.session = session;
        log.info("[WSClient-{}] Conectado. ID Sessão: {}", id, session.getId());

        session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
            @Override
            public void onMessage(ByteBuffer message) {
                handleBinaryMessage(message);
            }
        });

        processPendingSubscriptions();
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        this.session = null;
        log.warn("[WSClient-{}] Conexão fechada: {}", id, closeReason);

        pendingSubscriptions.addAll(activeChannels);
        activeChannels.clear();
        subscriptionCount.set(0);

        new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
            }
            connect();
        }).start();
    }

    @Override
    public void onError(Session session, Throwable throwable) {
        log.error("[WSClient-{}] Erro na sessão: {}", id, throwable.getMessage());
    }

    public void subscribe(String channel) {
        if (activeChannels.contains(channel) || pendingSubscriptions.contains(channel)) {
            return;
        }

        subscriptionCount.incrementAndGet();
        pendingSubscriptions.add(channel);
        processPendingSubscriptions();
    }

    public void unsubscribe(String channel) {
        if (isOpen()) {
            try {
                session.getBasicRemote().sendText(String.format("{\"method\":\"UNSUBSCRIPTION\",\"params\":[\"%s\"]}", channel));
                activeChannels.remove(channel);
                subscriptionCount.decrementAndGet();
            } catch (IOException e) {
                log.error("[WSClient-{}] Erro unsubscribe", id, e);
            }
        } else {
            pendingSubscriptions.remove(channel);
            activeChannels.remove(channel);
            subscriptionCount.decrementAndGet();
        }
    }

    public boolean isFull() {
        return subscriptionCount.get() >= MAX_SUBSCRIPTIONS;
    }

    public boolean hasChannel(String channel) {
        return activeChannels.contains(channel) || pendingSubscriptions.contains(channel);
    }

    public void sendPing() {
        if (isOpen()) {
            try {
                session.getBasicRemote().sendText("{\"method\":\"PING\"}");
            } catch (IOException e) {
                log.debug("[WSClient-{}] Erro ao enviar PING", id);
            }
        }
    }

    private boolean isOpen() {
        return this.session != null && this.session.isOpen();
    }

    private void processPendingSubscriptions() {
        if (!isOpen()) return;

        String channel;
        while ((channel = pendingSubscriptions.poll()) != null) {
            try {
                session.getBasicRemote().sendText(String.format("{\"method\":\"SUBSCRIPTION\",\"params\":[\"%s\"]}", channel));
                activeChannels.add(channel);
                log.info("[WSClient-{}] Inscrito no canal: {}", id, channel);
            } catch (IOException e) {
                log.error("[WSClient-{}] Erro ao inscrever no canal {}", id, channel, e);
                pendingSubscriptions.add(channel);
                return;
            }
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
            log.error("[WSClient-{}] Erro protobuf: {}", id, e.getMessage());
        } catch (Exception e) {
            log.error("[WSClient-{}] Erro genérico processando mensagem: {}", id, e.getMessage());
        }
    }

    private EventoCandleMexcDTO mapProtoToDto(PublicSpotKlineV3Api proto, String symbol) {
        return new EventoCandleMexcDTO(
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
}