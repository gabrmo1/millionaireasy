package br.com.bot_mexc.services.strategy.impl;

import br.com.bot_mexc.models.dtos.PrimitiveCandle;
import br.com.bot_mexc.services.strategy.IndicadorContext;
import br.com.bot_mexc.services.strategy.IndicadorState;
import br.com.bot_mexc.services.strategy.IndicadorStrategy;

/**
 * Base Abstrata para a Família RSI.
 * Garante que o JIT Compiler aplique Monomorphic Inline Caching nas subclasses
 * mantendo o path do cálculo 100% primitivo.
 */
public abstract class AbstractRsiStrategy implements IndicadorStrategy {

    protected abstract String getPeriodoParamKey();

    @Override
    public IndicadorState inicializarEstado() {
        return new RsiState();
    }

    @Override
    public double calcular(PrimitiveCandle candle, IndicadorContext context) {
        RsiState state = (RsiState) context.estado();
        int period = context.parametros().getOrDefault(getPeriodoParamKey(), 14);

        double previousClose = context.precoAnterior();

        // Fast-fail se for o primeiro tick de warmup e não há preço anterior
        if (Double.isNaN(previousClose)) {
            return Double.NaN;
        }

        // Simpatia Mecânica: Operações branchless preferenciais para o pipeline da CPU
        double change = candle.valorFechamento() - previousClose;
        double gain = Math.max(0.0, change);
        double loss = Math.max(0.0, -change);

        if (Double.isNaN(state.avgGain)) {
            state.avgGain = gain;
            state.avgLoss = loss;
        } else {
            // Wilder's Smoothing
            state.avgGain = ((state.avgGain * (period - 1.0)) + gain) / period;
            state.avgLoss = ((state.avgLoss * (period - 1.0)) + loss) / period;
        }

        // Hardware lock: Trava de divisão por zero (IEEE 754 compliance)
        if (state.avgLoss == 0.0) {
            return 100.0;
        }

        double rs = state.avgGain / state.avgLoss;
        return 100.0 - (100.0 / (1.0 + rs));
    }

    private static class RsiState implements IndicadorState {
        double avgGain = Double.NaN;
        double avgLoss = Double.NaN;

        @Override
        public void reset() {
            avgGain = Double.NaN;
            avgLoss = Double.NaN;
        }
    }
}