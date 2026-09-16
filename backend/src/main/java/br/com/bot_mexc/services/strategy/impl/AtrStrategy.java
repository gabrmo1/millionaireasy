package br.com.bot_mexc.services.strategy.impl;

import br.com.bot_mexc.models.dtos.PrimitiveCandle;
import br.com.bot_mexc.models.enums.TipoIndicador;
import br.com.bot_mexc.services.strategy.IndicadorContext;
import br.com.bot_mexc.services.strategy.IndicadorState;
import br.com.bot_mexc.services.strategy.IndicadorStrategy;

/**
 * ATR (Average True Range)
 * Volatilidade com Wilder's Smoothing.
 */
public class AtrStrategy implements IndicadorStrategy {

    public static final String PARAM_PERIOD = "atrPeriod";

    @Override
    public TipoIndicador getTipoIndicador() {
        return TipoIndicador.ATR;
    }

    @Override
    public IndicadorState inicializarEstado() {
        return new AtrState();
    }

    @Override
    public double calcular(PrimitiveCandle candle, IndicadorContext context) {
        AtrState state = (AtrState) context.estado();
        int period = context.parametros().getOrDefault(PARAM_PERIOD, 14);

        double high = candle.maxima();
        double low = candle.minima();
        double prevClose = context.precoAnterior();

        double tr = high - low;
        if (!Double.isNaN(prevClose) && prevClose > 0) {
            double tr2 = Math.abs(high - prevClose);
            double tr3 = Math.abs(low - prevClose);
            tr = Math.max(tr, Math.max(tr2, tr3));
        }

        if (Double.isNaN(state.atr)) {
            state.atr = tr; // Setup inicial
        } else {
            // Wilder's Smoothing Method
            state.atr = ((state.atr * (period - 1)) + tr) / period;
        }

        return state.atr;
    }

    private static class AtrState implements IndicadorState {
        double atr = Double.NaN;
        @Override
        public void reset() { atr = Double.NaN; }
    }
}