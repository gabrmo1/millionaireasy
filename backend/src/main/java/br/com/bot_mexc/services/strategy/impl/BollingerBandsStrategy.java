package br.com.bot_mexc.services.strategy.impl;

import br.com.bot_mexc.models.dtos.PrimitiveCandle;
import br.com.bot_mexc.models.enums.TipoIndicador;
import br.com.bot_mexc.services.strategy.IndicadorContext;
import br.com.bot_mexc.services.strategy.IndicadorState;
import br.com.bot_mexc.services.strategy.IndicadorStrategy;
import br.com.bot_mexc.utils.buffers.PrimitiveRingBuffer;

/**
 * Bollinger Bands
 * Integrado ao PrimitiveRingBuffer L1 (Zero GC allocation).
 */
public class BollingerBandsStrategy implements IndicadorStrategy {

    public static final String PARAM_PERIOD = "bbPeriod";
    public static final String PARAM_MULT = "bbMultiplier";
    public static final String PARAM_BAND = "bbBand"; // 1 = Upper, 0 = Middle, -1 = Lower

    @Override
    public TipoIndicador getTipoIndicador() {
        return TipoIndicador.BOLLINGER_BANDS;
    }

    @Override
    public IndicadorState inicializarEstado() {
        return new BollingerState(); // Capacidade lazy
    }

    @Override
    public double calcular(PrimitiveCandle candle, IndicadorContext context) {
        BollingerState state = (BollingerState) context.estado();
        int period = context.parametros().getOrDefault(PARAM_PERIOD, 20);
        double mult = context.parametros().getOrDefault(PARAM_MULT, 2);
        int band = context.parametros().getOrDefault(PARAM_BAND, 1);

        if (state.buffer == null || state.buffer.getCount() > period) {
            state.buffer = new PrimitiveRingBuffer(period);
        }

        state.buffer.add(candle.valorFechamento());

        if (!state.buffer.isFull()) return Double.NaN;

        double sma = state.buffer.getAverage();

        if (band == 0) return sma;

        double stdDev = state.buffer.getStandardDeviation();
        return band > 0 ? (sma + (stdDev * mult)) : (sma - (stdDev * mult));
    }

    private static class BollingerState implements IndicadorState {
        PrimitiveRingBuffer buffer;
        @Override
        public void reset() {
            if (buffer != null) buffer.reset();
        }
    }
}