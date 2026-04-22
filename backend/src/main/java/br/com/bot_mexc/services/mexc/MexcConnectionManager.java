package br.com.bot_mexc.services.mexc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class MexcConnectionManager {

    private final RabbitTemplate rabbitTemplate;

    @Value("${mexc.websocket.url}")
    private String websocketUrl;

    private final List<MexcWebSocketClient> clients = new CopyOnWriteArrayList<>();

    public synchronized void subscribe(String channel) {
        for (MexcWebSocketClient client : clients) {
            if (client.hasChannel(channel)) {
                return;
            }
        }

        Optional<MexcWebSocketClient> availableClient = clients.stream()
                .filter(c -> !c.isFull())
                .findFirst();

        MexcWebSocketClient targetClient;

        targetClient = availableClient
                .orElseGet(this::createNewClient);

        targetClient.subscribe(channel);
    }

    public synchronized void unsubscribe(String channel) {
        for (MexcWebSocketClient client : clients) {
            if (client.hasChannel(channel)) {
                client.unsubscribe(channel);
                break;
            }
        }
    }

    private MexcWebSocketClient createNewClient() {
        String clientId = String.valueOf(clients.size() + 1);
        log.info("Criando novo Cliente WebSocket (ID: {})...", clientId);

        MexcWebSocketClient newClient = new MexcWebSocketClient(clientId, websocketUrl, rabbitTemplate);
        newClient.connect();
        clients.add(newClient);

        return newClient;
    }

    @Scheduled(fixedRate = 20000)
    public void pingAllClients() {
        clients.forEach(MexcWebSocketClient::sendPing);
    }
}