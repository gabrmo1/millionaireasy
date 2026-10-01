package br.com.bot_mexc.modules.strategy.services.indicators;

import br.com.bot_mexc.modules.strategy.dtos.PrimitiveCandle;
import br.com.bot_mexc.shared.enums.TipoIndicador;
import org.springframework.stereotype.Component;

@Component
public class VolumeStrategy implements IndicadorStrategy {

    @Override
    public TipoIndicador getTipoIndicador() {
        return TipoIndicador.VOLUME;
    }

    @Override
    public IndicadorState inicializarEstado() {
        return null;
    }

    @Override
    public double calcular(PrimitiveCandle candle, IndicadorContext context) {
        return candle != null ? candle.volume() : 0.0;
    }
}
