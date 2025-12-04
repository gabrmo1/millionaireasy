package br.com.bot_mexc.services;

import br.com.bot_mexc.configs.RabbitMQConfig;
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
    public void registrarIntencaoDeCompra(String idOperacao, String par, BigDecimal precoAtual) {
        final var routingKey = "order.execute.buy." + par;
        final var payload = Map.of(
                "idOperacao", idOperacao,
                "par", par,
                "preco", precoAtual.toPlainString(),
                "tipo", "BUY"
        );

        log.info("SINAL DE COMPRA: Publicando intenção de compra para Operação {} (Par: {})", idOperacao, par);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDERS_ACTIONS_TOPIC,
                routingKey,
                payload
        );
    }

    @Async("asyncExecutor")
    public void registrarIntencaoDeVenda(String idOperacao, String par, BigDecimal precoAtual) {
        final var routingKey = "order.execute.sell." + par;
        final var payload = Map.of(
                "idOperacao", idOperacao,
                "par", par,
                "preco", precoAtual.toPlainString(),
                "tipo", "SELL"
        );

        log.info("SINAL DE VENDA: Publicando intenção de venda para Operação {} (Par: {})", idOperacao, par);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDERS_ACTIONS_TOPIC,
                routingKey,
                payload
        );
    }
}