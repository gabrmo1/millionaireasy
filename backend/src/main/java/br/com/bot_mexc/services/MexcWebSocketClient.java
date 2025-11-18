package br.com.bot_mexc.services;

import br.com.bot_mexc.proto.PushDataV3ApiWrapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import jakarta.websocket.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;

@Slf4j
@Component
public class MexcWebSocketClient {

    @Value("${mexc.websocket.url}")
    private String websocketUrl;

    private final WebSocketMessageHandlerService messageHandlerService;
    private final MexcSubscriptionService subscriptionService;
    private final WebSocketContainer container;

    private volatile boolean isConnecting = false;
    private int connectionAttempts = 0;
    private Session session;

    private static final int MAX_CONNECTION_ATTEMPTS = 5;
    private static final long RECONNECT_DELAY_MS = 10000;

    public MexcWebSocketClient(@Lazy MexcSubscriptionService subscriptionService,
                               WebSocketMessageHandlerService messageHandlerService,
                               @Value("${mexc.websocket.url}") String websocketUrl) {
        this.container = ContainerProvider.getWebSocketContainer();
        this.messageHandlerService = messageHandlerService;
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

                    log.info("==================== [ON_OPEN] Conexão WebSocket estabelecida (ID: {}) ====================", session.getId());

                    session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
                        @Override
                        public void onMessage(ByteBuffer message) {
                            MexcWebSocketClient.this.handleBinaryMessage(message);
                        }
                    });
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            MexcWebSocketClient.this.handleTextMessage(message);
                        }
                    });

                    try {
                        log.info("[ON_OPEN] Solicitando ressincronização de subscrições a partir do banco de dados...");
                        subscriptionService.resyncSubscriptionsFromDatabase();
                    } catch (Exception e) {
                        log.error("[ON_OPEN] Falha crítica durante a ressincronização das subscrições!", e);
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
            log.error("Falha ao conectar ao WebSocket: {}", e.getMessage(), e);
            isConnecting = false;
            if (connectionAttempts < MAX_CONNECTION_ATTEMPTS) {
                try {
                    Thread.sleep(RECONNECT_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                connect();
            } else {
                log.error("Número máximo de tentativas de reconexão atingido. Desistindo.");
            }
        }
    }

    private void handleBinaryMessage(ByteBuffer message) {
        try {
            final var bytes = new byte[message.remaining()];
            message.get(bytes);

            final var wrapper = PushDataV3ApiWrapper.parseFrom(bytes);

            if (!wrapper.hasPublicSpotKline()) {
                return;
            }

            final var klineData = wrapper.getPublicSpotKline();
            final var interval = klineData.getInterval();
            final var symbol = wrapper.getSymbol();

            final var klineJson = JsonFormat.printer().print(klineData);

            messageHandlerService.handleKlineMessage(klineJson, symbol, interval);

        } catch (InvalidProtocolBufferException e) {
            log.error("Falha ao decodificar mensagem Protobuf da MEXC. Verifique se os arquivos .proto estão corretos.", e);
        } catch (Exception e) {
            log.error("Erro inesperado ao processar mensagem ByteBuffer", e);
        }
    }

    private void handleTextMessage(String message) {
        if (message.contains("PONG")) {
            return;
        }
        log.info("Recebida mensagem de texto (inesperada): {}", message);
    }

    private void handleError(Throwable throwable) {
        log.error("==================== [ON_ERROR] Erro no WebSocket ====================", throwable);
    }

    private void handleClose(CloseReason closeReason) {
        log.warn("==================== [ON_CLOSE] Conexão WebSocket fechada (Code: {}, Reason: {}, Remote: {}) ====================",
                closeReason.getCloseCode(),
                closeReason.getReasonPhrase(),
                closeReason.getCloseCode() != CloseReason.CloseCodes.NORMAL_CLOSURE);

        this.session = null;
        if (closeReason.getCloseCode() != CloseReason.CloseCodes.NORMAL_CLOSURE) {
            log.info("Tentando reconectar devido a fechamento anormal...");
            connect();
        }
    }

    public void subscribe(String channel) {
        if (!isOpen()) {
            log.warn("WebSocket não está aberto. Tentando conectar antes de subscrever...");
            return;
        }
        try {
            final var params = String.format("{\"method\":\"SUBSCRIPTION\",\"params\":[\"%s\"]}", channel);
            log.info("Enviando subscrição: {}", params);
            session.getBasicRemote().sendText(params);
        } catch (IOException e) {
            log.error("Falha ao enviar mensagem de subscrição para {}: {}", channel, e.getMessage());
        }
    }

    public void unsubscribe(String channel) {
        if (!isOpen()) {
            log.warn("WebSocket não está aberto. Não é possível cancelar subscrição de {}", channel);
            return;
        }
        try {
            final var params = String.format("{\"method\":\"UNSUBSCRIPTION\",\"params\":[\"%s\"]}", channel);
            log.info("Enviando cancelamento de subscrição: {}", params);
            session.getBasicRemote().sendText(params);
        } catch (IOException e) {
            log.error("Falha ao enviar mensagem de cancelamento de subscrição para {}: {}", channel, e.getMessage());
        }
    }

    public boolean isOpen() {
        return this.session != null && this.session.isOpen();
    }

    @Scheduled(fixedRate = 20000)
    public void sendPing() {
        if (isOpen()) {
            try {
                session.getBasicRemote().sendText("""
                        {"method":"PING"}
                        """);
            } catch (IOException e) {
                log.warn("Falha ao enviar PING: {}", e.getMessage());
            }
        }
    }
}