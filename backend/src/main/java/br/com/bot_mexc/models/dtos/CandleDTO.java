package br.com.bot_mexc.models.dtos;

import lombok.With;

import java.math.BigDecimal;
import java.time.Instant;

@With
public record CandleDTO(
        Instant dataAbertura,
        Instant dataFechamento,
        BigDecimal valorAbertura,
        BigDecimal valorFechamento,
        BigDecimal minima,
        BigDecimal maxima,
        BigDecimal volume
) {
}