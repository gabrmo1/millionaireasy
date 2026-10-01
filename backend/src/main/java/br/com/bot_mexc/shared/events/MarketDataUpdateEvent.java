package br.com.bot_mexc.shared.events;

import java.math.BigDecimal;

public record MarketDataUpdateEvent(
        String par,
        String intervalo,
        BigDecimal precoAbertura,
        BigDecimal precoFechamento,
        BigDecimal precoMinimo,
        BigDecimal precoMaximo,
        BigDecimal volume,
        long dataAbertura,
        long dataFechamento,
        boolean isFechado
) {}
