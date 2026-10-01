package br.com.bot_mexc;

import br.com.bot_mexc.modules.strategy.dtos.OperacaoCacheDTO;
import br.com.bot_mexc.modules.strategy.services.OperacaoCacheService;
import br.com.bot_mexc.modules.oms.services.MexcOrderDispatchService;
import br.com.bot_mexc.modules.oms.services.PreTradeRiskService;
import br.com.bot_mexc.modules.oms.services.TradeSignalListener;
import br.com.bot_mexc.modules.strategy.services.SimulacaoService;
import br.com.bot_mexc.shared.events.TradeSignalEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeSignalListenerTest {

    @Mock
    private OperacaoCacheService operacaoCacheService;

    @Mock
    private SimulacaoService simulacaoService;

    @Mock
    private PreTradeRiskService preTradeRiskService;

    @Mock
    private MexcOrderDispatchService mexcOrderDispatchService;

    @InjectMocks
    private TradeSignalListener tradeSignalListener;

    @Test
    @DisplayName("Deve processar evento in-memory de sinal de compra em modo teste")
    void deveProcessarSinalDeCompraEmModoTeste() {
        String idOperacao = "op-123";
        String par = "BTCUSDT";
        String intervalo = "15m";
        BigDecimal preco = new BigDecimal("65000.00");

        OperacaoCacheDTO operacaoCache = new OperacaoCacheDTO(
                idOperacao,
                par,
                intervalo,
                "operador-1",
                "est-1",
                true, // modoTeste = true
                new BigDecimal("1000.00"),
                false,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("100.00"),
                null,
                "USDT",
                false,
                BigDecimal.ZERO,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );

        when(operacaoCacheService.getOperacaoById(idOperacao, par, intervalo)).thenReturn(operacaoCache);
        when(preTradeRiskService.validarRiscoPreTrade(eq(operacaoCache), any())).thenReturn(true);

        TradeSignalEvent event = new TradeSignalEvent(
                idOperacao,
                par,
                intervalo,
                TradeSignalEvent.TipoSinal.COMPRA,
                preco,
                Map.of("RSI", new BigDecimal("28.5")),
                System.currentTimeMillis(),
                Instant.now()
        );

        // Executa o listener in-memory
        tradeSignalListener.onTradeSignal(event);

        // Valida que a ordem simulada foi processada diretamente sem rede
        verify(simulacaoService, times(1)).processarOrdemSimulada(eq(operacaoCache), any());
        verify(mexcOrderDispatchService, never()).despacharOrdemMercado(any(), any());
    }
}
