package br.com.bot_mexc.modules.oms.services;

import br.com.bot_mexc.modules.strategy.dtos.OperacaoCacheDTO;
import br.com.bot_mexc.modules.strategy.services.OperacaoCacheService;
import br.com.bot_mexc.modules.oms.dtos.OrdemRequestDTO;
import br.com.bot_mexc.modules.strategy.services.SimulacaoService;
import br.com.bot_mexc.shared.events.TradeSignalEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeSignalListener {

    private final OperacaoCacheService operacaoCacheService;
    private final SimulacaoService simulacaoService;
    private final PreTradeRiskService preTradeRiskService;
    private final MexcOrderDispatchService mexcOrderDispatchService;

    @EventListener
    public void onTradeSignal(TradeSignalEvent event) {
        log.info("[ZERO-LATENCY OMS] Sinal {} recebido em memória para Operação ID: {} ({}/{}). Preço: {}",
                event.tipo(), event.idOperacao(), event.par(), event.intervalo(), event.preco());

        try {
            OperacaoCacheDTO operacaoCache = operacaoCacheService.getOperacaoById(
                    event.idOperacao(),
                    event.par(),
                    event.intervalo()
            );

            if (operacaoCache == null) {
                log.error("Operação {} não encontrada no cache para {}/{}.",
                        event.idOperacao(), event.par(), event.intervalo());
                return;
            }

            // 1. Gestão de Risco Pré-Trade
            if (!preTradeRiskService.validarRiscoPreTrade(operacaoCache, event)) {
                return;
            }

            // 2. Despacho conforme modo operacional (Simulado vs Real)
            if (Boolean.TRUE.equals(operacaoCache.modoTeste())) {
                OrdemRequestDTO.TipoOrdem tipoOrdem = (event.tipo() == TradeSignalEvent.TipoSinal.COMPRA)
                        ? OrdemRequestDTO.TipoOrdem.BUY
                        : OrdemRequestDTO.TipoOrdem.SELL;

                OrdemRequestDTO ordem = new OrdemRequestDTO(
                        event.idOperacao(),
                        event.par(),
                        event.intervalo(),
                        event.preco(),
                        tipoOrdem,
                        event.indicadores(),
                        event.dataCandle()
                );
                simulacaoService.processarOrdemSimulada(operacaoCache, ordem);
            } else {
                log.info("[REAL TRADE] Despachando ordem real assinada para a MEXC. Operação ID: {}", operacaoCache.id());
                mexcOrderDispatchService.despacharOrdemMercado(operacaoCache, event);
            }

        } catch (Exception e) {
            log.error("Erro ao processar execução de ordem in-memory para operação {}: {}", event.idOperacao(), e.getMessage(), e);
        }
    }
}
