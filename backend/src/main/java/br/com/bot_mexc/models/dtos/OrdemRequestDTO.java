package br.com.bot_mexc.models.dtos;

import java.math.BigDecimal;
import java.util.Map;

public record OrdemRequestDTO(
        String idOperacao,
        String par,
        String intervalo,
        BigDecimal preco,
        TipoOrdem tipo,
        Map<String, BigDecimal> indicadores,
        long dataCandle
) {
    public enum TipoOrdem {
        BUY, SELL
    }
}