package br.com.bot_mexc.shared.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record TradeSignalEvent(
        String idOperacao,
        String par,
        String intervalo,
        TipoSinal tipo,
        BigDecimal preco,
        Map<String, BigDecimal> indicadores,
        long dataCandle,
        Instant timestamp
) {
    public enum TipoSinal {
        COMPRA, VENDA
    }
}