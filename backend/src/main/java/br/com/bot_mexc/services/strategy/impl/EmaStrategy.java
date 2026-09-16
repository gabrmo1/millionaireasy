package br.com.bot_mexc.services.strategy.impl;

import br.com.bot_mexc.models.dtos.PrimitiveCandle;
import br.com.bot_mexc.models.enums.TipoIndicador;
import br.com.bot_mexc.services.strategy.IndicadorContext;
import br.com.bot_mexc.services.strategy.IndicadorState;
import br.com.bot_mexc.services.strategy.IndicadorStrategy;
import br.com.bot_mexc.utils.constants.IndicadorKeys;
import org.springframework.stereotype.Component;

@Component
public class EmaStrategy implements IndicadorStrategy {

    @Override
    public TipoIndicador getTipoIndicador() {
        return TipoIndicador.EMA;
    }

    @Override
    public IndicadorState inicializarEstado() {
        return new EmaState();
    }

    @Override
    public double calcular(PrimitiveCandle candle, IndicadorContext context) {
        EmaState state = (EmaState) context.estado();
        int period = context.parametros().getOrDefault(IndicadorKeys.PARAM_PERIODO_EMA, 200);
        double currentClose = candle.valorFechamento();

        // Monomorphic fast-path: Setup inicial direto no primitive
        if (Double.isNaN(state.emaAnterior)) {
            state.emaAnterior = currentClose;
            return currentClose;
        }

        // Multiplicador com constant folding garantido pelo JIT
        double multiplier = 2.0 / (period + 1.0);
        state.emaAnterior = (currentClose - state.emaAnterior) * multiplier + state.emaAnterior;

        return state.emaAnterior;
    }

    private static class EmaState implements IndicadorState {
        double emaAnterior = Double.NaN;

        @Override
        public void reset() {
            emaAnterior = Double.NaN;
        }
    }
}