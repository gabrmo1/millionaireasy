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
import br.com.bot_mexc.shared.constants.IndicadorKeys;
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