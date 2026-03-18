package br.com.bot_mexc.models.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record OrdemRequestDTO(
        String idOperacao,
        String par,
        String intervalo,
        BigDecimal preco,
        TipoOrdem tipo,
        Map<String, BigDecimal> indicadores,
        Instant dataCandle
) {
    public enum TipoOrdem {
        BUY, SELL
    }
}