package br.com.bot_mexc.modules.oms.consumers;
import br.com.bot_mexc.shared.enums.*;
import br.com.bot_mexc.shared.configs.RabbitMQConfig;
import br.com.bot_mexc.modules.oms.services.*;
import br.com.bot_mexc.modules.oms.dtos.*;
import br.com.bot_mexc.modules.oms.integrations.*;
import br.com.bot_mexc.modules.strategy.services.OperacaoCacheService;
import br.com.bot_mexc.modules.strategy.dtos.OperacaoCacheDTO;
import br.com.bot_mexc.modules.strategy.services.SimulacaoService;

import br.com.bot_mexc.shared.configs.RabbitMQConfig;
import br.com.bot_mexc.modules.strategy.dtos.OperacaoCacheDTO;
import br.com.bot_mexc.modules.oms.dtos.OrdemRequestDTO;
import br.com.bot_mexc.modules.strategy.services.OperacaoCacheService;
import br.com.bot_mexc.modules.strategy.services.SimulacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExecutionConsumer {

    private final OperacaoCacheService operacaoCacheService;
    private final SimulacaoService simulacaoService;

    @RabbitListener(queues = RabbitMQConfig.ORDERS_SIMULATE_QUEUE)
    public void consumirOrdem(OrdemRequestDTO ordem) {
        try {
            OperacaoCacheDTO operacaoCache = operacaoCacheService.getOperacaoById(
                    ordem.idOperacao(),
                    ordem.par(),
                    ordem.intervalo()
            );

            if (operacaoCache == null) {
                log.error("Operação {} não encontrada no cache para {}/{}.",
                        ordem.idOperacao(), ordem.par(), ordem.intervalo());
                return;
            }

            if (Boolean.TRUE.equals(operacaoCache.modoTeste())) {
                simulacaoService.processarOrdemSimulada(operacaoCache, ordem);
            } else {
                log.info("[REAL TRADE] Recebido para conta REAL. Cache ID: {}", operacaoCache.id());
                // TODO: Chamada HTTP para MEXC
            }

        } catch (Exception e) {
            log.error("Erro ao processar execução de ordem: {}", e.getMessage(), e);
        }
    }
}