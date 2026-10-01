package br.com.bot_mexc.modules.oms.services;

import br.com.bot_mexc.modules.oms.utils.MexcSignatureHelper;
import br.com.bot_mexc.modules.strategy.entities.Operador;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MexcPrivateUserDataStreamService {

    private final ObjectMapper objectMapper;
    private static final String WS_URL = "wss://wbs-api.mexc.com/ws";

    private final Map<String, Session> activeSessions = new ConcurrentHashMap<>();

    public synchronized void conectarStreamPrivado(Operador operador) {
        if (operador == null || operador.getAccessKey() == null) {
            return;
        }

        if (activeSessions.containsKey(operador.getId()) && activeSessions.get(operador.getId()).isOpen()) {
            log.info("[MEXC PRIVATE WS] Sessão privada já ativa para operador: {}", operador.getNome());
            return;
        }

        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    activeSessions.put(operador.getId(), session);
                    log.info("[MEXC PRIVATE WS] Conexão WebSocket estabelecida com sucesso para Operador: {}", operador.getNome());

                    // Configura listener de mensagens de execução
                    session.addMessageHandler(String.class, message -> processarMensagemPrivada(operador, message));

                    // Envia mensagem de login assinado para assinar spot@private.orders.v3.api
                    assinarOrdensPrivadas(session, operador);
                }

                @Override
                public void onClose(Session session, CloseReason closeReason) {
                    activeSessions.remove(operador.getId());
                    log.warn("[MEXC PRIVATE WS] Conexão privada encerrada para {}: {}", operador.getNome(), closeReason.getReasonPhrase());
                }

                @Override
                public void onError(Session session, Throwable throwable) {
                    log.error("[MEXC PRIVATE WS] Erro na sessão privada de {}: {}", operador.getNome(), throwable.getMessage());
                }
            }, ClientEndpointConfig.Builder.create().build(), URI.create(WS_URL));

        } catch (Exception e) {
            log.error("[MEXC PRIVATE WS] Falha ao conectar WebSocket privado para {}: {}", operador.getNome(), e.getMessage());
        }
    }

    public synchronized void desconectarTodos() {
        activeSessions.forEach((operadorId, session) -> {
            try {
                if (session.isOpen()) {
                    session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Kill Switch Acionado"));
                }
            } catch (Exception e) {
                log.error("[MEXC PRIVATE WS] Erro ao fechar sessão {}: {}", operadorId, e.getMessage());
            }
        });
        activeSessions.clear();
        log.info("[MEXC PRIVATE WS] Todas as conexões WebSocket privadas foram encerradas.");
    }

    private void assinarOrdensPrivadas(Session session, Operador operador) {
        try {
            final long reqTime = System.currentTimeMillis();
            final String dataToSign = operador.getAccessKey() + reqTime;
            final String signature = MexcSignatureHelper.signHmacSha256(dataToSign, operador.getSecretKey());

            String payload = String.format("""
                {
                    "method": "SUBSCRIPTION",
                    "params": ["spot@private.orders.v3.api"],
                    "apiKey": "%s",
                    "reqTime": "%d",
                    "signature": "%s"
                }
            """, operador.getAccessKey(), reqTime, signature);

            session.getBasicRemote().sendText(payload);
            log.info("[MEXC PRIVATE WS] Subscrição assinada enviada para canal spot@private.orders.v3.api (Operador: {})", operador.getNome());

        } catch (Exception e) {
            log.error("[MEXC PRIVATE WS] Falha ao enviar subscrição autenticada: {}", e.getMessage(), e);
        }
    }

    private void processarMensagemPrivada(Operador operador, String message) {
        try {
            JsonNode root = objectMapper.readTree(message);

            if (root.has("c") && "spot@private.orders.v3.api".equals(root.get("c").asText())) {
                JsonNode data = root.path("d");
                String symbol = data.path("s").asText();
                String side = data.path("S").asText(); // 1: BUY, 2: SELL
                String status = data.path("s").asText(); // 2: FILLED, 4: CANCELED, etc.
                String orderId = data.path("i").asText();
                String clientOrderId = data.path("c").asText();

                log.info("[EXECUÇÃO CONFIRMADA] MEXC Private Stream -> Operador: {}, Par: {}, Ordem: {}, Status: {}, ClientOrderId: {}",
                        operador.getNome(), symbol, orderId, status, clientOrderId);
            } else if (root.has("code") && root.get("code").asInt() == 0) {
                log.info("[MEXC PRIVATE WS] Confirmação de subscrição recebida: {}", message);
            }
        } catch (Exception e) {
            log.debug("[MEXC PRIVATE WS] Mensagem não tratada: {}", message);
        }
    }
}
