package br.com.bot_mexc.models.dtos;

/**
 * DTO transitório focado no Hot-Path do WebSocket (Tick-by-Tick).
 * Substitui o CandleDTO baseado em BigDecimal durante os cálculos em tempo real.
 * A conversão para BigDecimal ocorre APENAS no disparo da ordem ou persistência de fechamento (Task 4).
 */
public record PrimitiveCandle(
        long dataAbertura,
        long dataFechamento,
        double valorAbertura,
        double valorFechamento,
        double minima,
        double maxima,
        double volume
) {}