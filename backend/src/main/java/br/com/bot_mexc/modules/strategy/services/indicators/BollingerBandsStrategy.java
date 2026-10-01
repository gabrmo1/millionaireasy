package br.com.bot_mexc.modules.strategy.services.indicators;
import br.com.bot_mexc.shared.enums.*;
import br.com.bot_mexc.shared.utils.DateUtils;
import br.com.bot_mexc.shared.constants.IndicadorKeys;
import br.com.bot_mexc.shared.configs.RabbitMQConfig;
import br.com.bot_mexc.shared.configs.RedisConfig;
import br.com.bot_mexc.modules.market.services.*;
import br.com.bot_mexc.modules.market.services.mexc.*;
import br.com.bot_mexc.modules.strategy.services.indicators.*;
import br.com.bot_mexc.modules.market.dtos.*;
import br.com.bot_mexc.modules.market.dtos.mexc.*;
import br.com.bot_mexc.modules.strategy.dtos.monitoramento.*;
import br.com.bot_mexc.modules.strategy.entities.CondicaoCompra;
import br.com.bot_mexc.modules.strategy.entities.CondicaoVenda;
import br.com.bot_mexc.modules.strategy.entities.IndicadorConfig;
import br.com.bot_mexc.modules.strategy.entities.Operacao;
import br.com.bot_mexc.modules.strategy.repositories.OperacaoRepository;
import br.com.bot_mexc.modules.strategy.repositories.CompraRepository;
import br.com.bot_mexc.modules.strategy.repositories.VendaRepository;
import br.com.bot_mexc.modules.timeseries.dtos.CandleDTO;
import br.com.bot_mexc.modules.timeseries.services.CandleService;
import br.com.bot_mexc.modules.timeseries.services.AnaliseService;
import br.com.bot_mexc.modules.timeseries.repositories.AnaliseRepository;
import br.com.bot_mexc.modules.timeseries.builders.AnaliseBuilder;


import br.com.bot_mexc.modules.strategy.dtos.PrimitiveCandle;
import br.com.bot_mexc.shared.enums.TipoIndicador;
import br.com.bot_mexc.modules.strategy.services.indicators.IndicadorContext;
import br.com.bot_mexc.modules.strategy.services.indicators.IndicadorState;
import br.com.bot_mexc.modules.strategy.services.indicators.IndicadorStrategy;
import br.com.bot_mexc.shared.buffers.PrimitiveRingBuffer;

/**
 * Bollinger Bands
 * Integrado ao PrimitiveRingBuffer L1 (Zero GC allocation).
 */
@org.springframework.stereotype.Component
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