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

/**
 * ATR (Average True Range)
 * Volatilidade com Wilder's Smoothing.
 */
@org.springframework.stereotype.Component
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