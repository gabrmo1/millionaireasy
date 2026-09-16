package br.com.bot_mexc.services.strategy.impl;

import br.com.bot_mexc.models.dtos.PrimitiveCandle;
import br.com.bot_mexc.models.enums.TipoIndicador;
import br.com.bot_mexc.services.strategy.IndicadorContext;
import br.com.bot_mexc.services.strategy.IndicadorState;
import br.com.bot_mexc.services.strategy.IndicadorStrategy;

import java.time.Instant;
import java.time.ZoneOffset;

/**
 * VWAP (Volume Weighted Average Price)
 * Rastreamento de preço típico ponderado, com reset por sessão diária.
 */
public class VwapStrategy implements IndicadorStrategy {

    @Override
    public TipoIndicador getTipoIndicador() {
        return TipoIndicador.VWAP;
    }

    @Override
    public IndicadorState inicializarEstado() {
        return new VwapState();
    }

    @Override
    public double calcular(PrimitiveCandle candle, IndicadorContext context) {
        VwapState state = (VwapState) context.estado();

        // Verifica Turnover de Sessão (Reset Diário UTC)
        long currentDay = Instant.ofEpochSecond(candle.dataAbertura()).atZone(ZoneOffset.UTC).getDayOfYear();
        if (state.lastDay != currentDay) {
            state.reset();
            state.lastDay = currentDay;
        }

        double typicalPrice = (candle.maxima() + candle.minima() + candle.valorFechamento()) / 3.0;
        double volume = candle.volume();

        state.cumTypicalVolume += (typicalPrice * volume);
        state.cumVolume += volume;

        return state.cumVolume == 0.0 ? typicalPrice : (state.cumTypicalVolume / state.cumVolume);
    }

    private static class VwapState implements IndicadorState {
        double cumTypicalVolume = 0.0;
        double cumVolume = 0.0;
        long lastDay = -1;

        @Override
        public void reset() {
            cumTypicalVolume = 0.0;
            cumVolume = 0.0;
        }
    }
}