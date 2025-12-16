package br.com.bot_mexc.models.dtos;

import java.math.BigDecimal;

public record OrdemRequestDTO(
        String idOperacao,
        String par,
        String intervalo,
        BigDecimal preco,
        TipoOrdem tipo
) {
    public enum TipoOrdem {
        BUY, SELL
    }
}