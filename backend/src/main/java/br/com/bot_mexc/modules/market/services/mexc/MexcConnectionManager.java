package br.com.bot_mexc.modules.market.services.mexc;
import br.com.bot_mexc.shared.enums.*;
import br.com.bot_mexc.shared.utils.DateUtils;
import br.com.bot_mexc.shared.constants.IndicadorKeys;
import br.com.bot_mexc.shared.configs.RabbitMQConfig;
import br.com.bot_mexc.shared.configs.RedisConfig;
import br.com.bot_mexc.modules.market.services.*;
import br.com.bot_mexc.modules.market.services.mexc.*;
import br.com.bot_mexc.modules.strategy.services.indicators.*;
import br.com.bot_mexc.modules.market.dtos.*;
import br.com.bot_mexc.modules.market.dtos.mexc.*;
import br.com.bot_mexc.modules.strategy.dtos.monitoramento.*;
import br.com.bot_mexc.modules.strategy.entities.CondicaoCompra;
import br.com.bot_mexc.modules.strategy.entities.CondicaoVenda;
import br.com.bot_mexc.modules.strategy.entities.IndicadorConfig;
import br.com.bot_mexc.modules.strategy.entities.Operacao;
import br.com.bot_mexc.modules.strategy.repositories.OperacaoRepository;
import br.com.bot_mexc.modules.strategy.repositories.CompraRepository;
import br.com.bot_mexc.modules.strategy.repositories.VendaRepository;
import br.com.bot_mexc.modules.timeseries.dtos.CandleDTO;
import br.com.bot_mexc.modules.timeseries.services.CandleService;
import br.com.bot_mexc.modules.timeseries.services.AnaliseService;
import br.com.bot_mexc.modules.timeseries.repositories.AnaliseRepository;
import br.com.bot_mexc.modules.timeseries.builders.AnaliseBuilder;


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