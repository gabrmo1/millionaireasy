package br.com.bot_mexc.services.strategy.impl;

import br.com.bot_mexc.models.dtos.PrimitiveCandle;
import br.com.bot_mexc.models.enums.TipoIndicador;
import br.com.bot_mexc.services.strategy.IndicadorContext;
import br.com.bot_mexc.services.strategy.IndicadorState;
import br.com.bot_mexc.services.strategy.IndicadorStrategy;
import br.com.bot_mexc.utils.buffers.PrimitiveRingBuffer;
import br.com.bot_mexc.utils.constants.IndicadorKeys;
import org.springframework.stereotype.Component;

@Component
public class SmaStrategy implements IndicadorStrategy {

    @Override
    public TipoIndicador getTipoIndicador() {
        return TipoIndicador.SMA;
    }

    @Override
    public IndicadorState inicializarEstado() {
        return new SmaState();
    }

    @Override
    public double calcular(PrimitiveCandle candle, IndicadorContext context) {
        SmaState state = (SmaState) context.estado();
        int period = context.parametros().getOrDefault(IndicadorKeys.PARAM_PERIODO_SMA, 200);

        // Pre-alocação Lazy confinada (evita L1 cache misses no RingBuffer)
        if (state.buffer == null) {
            state.buffer = new PrimitiveRingBuffer(period);
        }

        state.buffer.add(candle.valorFechamento());
        return state.buffer.getAverage();
    }

    private static class SmaState implements IndicadorState {
        PrimitiveRingBuffer buffer;

        @Override
        public void reset() {
            if (buffer != null) buffer.reset();
        }
    }
}