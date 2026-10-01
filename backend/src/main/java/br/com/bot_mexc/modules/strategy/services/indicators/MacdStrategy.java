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
import lombok.RequiredArgsConstructor;

/**
 * MACD (Moving Average Convergence Divergence)
 * Otimizado com primitivos para evitar alocação de BigDecimals.
 */
@org.springframework.stereotype.Component
public class MacdStrategy implements IndicadorStrategy {

    public static final String PARAM_FAST = "macdFast";
    public static final String PARAM_SLOW = "macdSlow";
    public static final String PARAM_SIGNAL = "macdSignal";

    // 0 = MACD Line, 1 = Signal Line, 2 = Histogram
    public static final String PARAM_RETURN_TYPE = "macdReturnType";

    @Override
    public TipoIndicador getTipoIndicador() {
        return TipoIndicador.MACD;
    }

    @Override
    public IndicadorState inicializarEstado() {
        return new MacdState();
    }

    @Override
    public double calcular(PrimitiveCandle candle, IndicadorContext context) {
        MacdState state = (MacdState) context.estado();
        double close = candle.valorFechamento();

        int fastP = context.parametros().getOrDefault(PARAM_FAST, 12);
        int slowP = context.parametros().getOrDefault(PARAM_SLOW, 26);
        int sigP = context.parametros().getOrDefault(PARAM_SIGNAL, 9);
        int returnType = context.parametros().getOrDefault(PARAM_RETURN_TYPE, 0);

        state.emaFast = calcularEma(close, state.emaFast, fastP);
        state.emaSlow = calcularEma(close, state.emaSlow, slowP);

        double macdLine = state.emaFast - state.emaSlow;

        // Evita calcular o signal se as EMAs ainda estão em warmup (NaN)
        if (!Double.isNaN(macdLine)) {
            state.emaSignal = calcularEma(macdLine, state.emaSignal, sigP);
        }

        return switch (returnType) {
            case 1 -> state.emaSignal;
            case 2 -> macdLine - (Double.isNaN(state.emaSignal) ? 0.0 : state.emaSignal);
            default -> macdLine;
        };
    }

    private double calcularEma(double preco, double emaAnterior, int periodo) {
        if (Double.isNaN(emaAnterior)) return preco;
        double multiplicador = 2.0 / (periodo + 1.0);
        return (preco - emaAnterior) * multiplicador + emaAnterior;
    }

    private static class MacdState implements IndicadorState {
        double emaFast = Double.NaN;
        double emaSlow = Double.NaN;
        double emaSignal = Double.NaN;

        @Override
        public void reset() {
            emaFast = Double.NaN;
            emaSlow = Double.NaN;
            emaSignal = Double.NaN;
        }
    }
}