package br.com.bot_mexc.models.dtos;

import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@With
public record CandleDTO(
        LocalDateTime dataAbertura,
        LocalDateTime dataFechamento,
        BigDecimal valorAbertura,
        BigDecimal valorFechamento,
        BigDecimal minima,
        BigDecimal maxima,
        BigDecimal volume
) {
}