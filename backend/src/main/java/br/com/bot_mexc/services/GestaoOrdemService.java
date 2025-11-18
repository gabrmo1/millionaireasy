package br.com.bot_mexc.services;

import br.com.bot_mexc.configs.RabbitMQConfig;
import br.com.bot_mexc.models.entities.Operacao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GestaoOrdemService {

    private final RabbitTemplate rabbitTemplate;

    @Async("asyncExecutor")
    public void registrarIntencaoDeCompra(Operacao operacao, BigDecimal precoAtual) {
        String routingKey = "order.execute.buy." + operacao.getPar();

        Map<String, String> payload = Map.of(
                "idOperacao", operacao.getId(),
                "par", operacao.getPar(),
                "preco", precoAtual.toPlainString(),
                "tipo", "BUY"
        );

        log.info("SINAL DE COMPRA: Publicando intenção de compra para Operação {} (Par: {})", operacao.getId(), operacao.getPar());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDERS_ACTIONS_TOPIC,
                routingKey,
                payload
        );
    }

    @Async("asyncExecutor")
    public void registrarIntencaoDeVenda(Operacao operacao, BigDecimal precoAtual) {
        String routingKey = "order.execute.sell." + operacao.getPar();

        Map<String, String> payload = Map.of(
                "idOperacao", operacao.getId(),
                "par", operacao.getPar(),
                "preco", precoAtual.toPlainString(),
                "tipo", "SELL"
        );

        log.info("SINAL DE VENDA: Publicando intenção de venda para Operação {} (Par: {})", operacao.getId(), operacao.getPar());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDERS_ACTIONS_TOPIC,
                routingKey,
                payload
        );
    }
}