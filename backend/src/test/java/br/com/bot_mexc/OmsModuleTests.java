package br.com.bot_mexc;

import br.com.bot_mexc.modules.strategy.dtos.OperacaoCacheDTO;
import br.com.bot_mexc.modules.oms.services.MexcRateLimiter;
import br.com.bot_mexc.modules.oms.services.PreTradeRiskService;
import br.com.bot_mexc.modules.oms.utils.MexcSignatureHelper;
import br.com.bot_mexc.shared.events.TradeSignalEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OmsModuleTests {

    private final MexcRateLimiter rateLimiter = new MexcRateLimiter();
    private final PreTradeRiskService riskService = new PreTradeRiskService();

    @Test
    @DisplayName("MexcRateLimiter deve adquirir tokens dentro do limite")
    void deveAdquirirTokenNoRateLimiter() {
        assertTrue(rateLimiter.tryAcquire(), "Deveria conseguir adquirir o primeiro token");
    }

    @Test
    @DisplayName("MexcSignatureHelper deve calcular hash HMAC-SHA256 determinístico")
    void deveCalcularAssinaturaHmacSha256() {
        String data = "symbol=BTCUSDT&timestamp=1700000000000";
        String secret = "minhaChaveSecretaMexc";

        String signature1 = MexcSignatureHelper.signHmacSha256(data, secret);
        String signature2 = MexcSignatureHelper.signHmacSha256(data, secret);

        assertNotNull(signature1);
        assertEquals(64, signature1.length(), "Assinatura SHA-256 em hex deve ter 64 caracteres");
        assertEquals(signature1, signature2, "Assinatura com os mesmos parâmetros deve ser idêntica");
    }

    @Test
    @DisplayName("PreTradeRiskService deve rejeitar compra se robô já estiver posicionado")
    void deveRejeitarCompraSeJaPosicionado() {
        OperacaoCacheDTO operacao = new OperacaoCacheDTO(
                "op-1", "BTCUSDT", "15m", "opd-1", "est-1", false,
                new BigDecimal("500"),
                true, // posicionado = true
                new BigDecimal("60000"), new BigDecimal("0.01"),
                new BigDecimal("50"), null, "USDT", false, BigDecimal.ZERO,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
        );

        TradeSignalEvent sinal = new TradeSignalEvent(
                "op-1", "BTCUSDT", "15m", TradeSignalEvent.TipoSinal.COMPRA,
                new BigDecimal("61000"), Map.of(), System.currentTimeMillis(), Instant.now()
        );

        boolean aprovado = riskService.validarRiscoPreTrade(operacao, sinal);
        assertFalse(aprovado, "Não deve permitir nova compra se a operação já estiver posicionada");
    }

    @Test
    @DisplayName("PreTradeRiskService deve aprovar compra válida para robô desposicionado")
    void deveAprovarCompraValida() {
        OperacaoCacheDTO operacao = new OperacaoCacheDTO(
                "op-1", "BTCUSDT", "15m", "opd-1", "est-1", false,
                new BigDecimal("500"),
                false, // posicionado = false
                BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("50"), null, "USDT", false, BigDecimal.ZERO,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
        );

        TradeSignalEvent sinal = new TradeSignalEvent(
                "op-1", "BTCUSDT", "15m", TradeSignalEvent.TipoSinal.COMPRA,
                new BigDecimal("61000"), Map.of(), System.currentTimeMillis(), Instant.now()
        );

        boolean aprovado = riskService.validarRiscoPreTrade(operacao, sinal);
        assertTrue(aprovado, "Deve aprovar compra se robô não estiver posicionado e valor for superior a 5 USDT");
    }

    @Test
    @DisplayName("PreTradeRiskService deve rejeitar venda se robô não estiver posicionado")
    void deveRejeitarVendaSemCustodia() {
        OperacaoCacheDTO operacao = new OperacaoCacheDTO(
                "op-1", "BTCUSDT", "15m", "opd-1", "est-1", false,
                new BigDecimal("500"),
                false, // posicionado = false
                BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("50"), null, "USDT", false, BigDecimal.ZERO,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
        );

        TradeSignalEvent sinal = new TradeSignalEvent(
                "op-1", "BTCUSDT", "15m", TradeSignalEvent.TipoSinal.VENDA,
                new BigDecimal("61000"), Map.of(), System.currentTimeMillis(), Instant.now()
        );

        boolean aprovado = riskService.validarRiscoPreTrade(operacao, sinal);
        assertFalse(aprovado, "Não deve permitir venda se o robô não possuir ativos em custódia");
    }
}
