package br.com.bot_mexc.services;

import br.com.bot_mexc.configs.RabbitMQConfig;
import br.com.bot_mexc.models.dtos.OrdemRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class GestaoOrdemService {

    private final RabbitTemplate rabbitTemplate;

    @Async("asyncExecutor")
    public void registrarIntencaoDeCompra(String idOperacao, String par, String intervalo, BigDecimal precoAtual) {
        enviarOrdem(idOperacao, par, intervalo, precoAtual, OrdemRequestDTO.TipoOrdem.BUY);
    }

    @Async("asyncExecutor")
    public void registrarIntencaoDeVenda(String idOperacao, String par, String intervalo, BigDecimal precoAtual) {
        enviarOrdem(idOperacao, par, intervalo, precoAtual, OrdemRequestDTO.TipoOrdem.SELL);
    }

    private void enviarOrdem(String idOperacao, String par, String intervalo, BigDecimal preco, OrdemRequestDTO.TipoOrdem tipo) {
        final var routingKey = "order.execute." + tipo.name().toLowerCase() + "." + par;
        // Agora passando o intervalo corretamente no DTO
        final var payload = new OrdemRequestDTO(idOperacao, par, intervalo, preco, tipo);

        log.info("SINAL DE {}: Publicando intenção para Operação {} (Par: {}, Intervalo: {}, Preço: {})",
                tipo, idOperacao, par, intervalo, preco);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDERS_ACTIONS_TOPIC,
                routingKey,
                payload
        );
    }
}