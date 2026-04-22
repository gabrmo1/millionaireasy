package br.com.bot_mexc.models.dtos;

import lombok.With;

import java.math.BigDecimal;

@With
public record CandleDTO(
        long dataAbertura,
        long dataFechamento,
        BigDecimal valorAbertura,
        BigDecimal valorFechamento,
        BigDecimal minima,
        BigDecimal maxima,
        BigDecimal volume
) {
}