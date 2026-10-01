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

import java.time.Instant;
import java.time.ZoneOffset;

/**
 * VWAP (Volume Weighted Average Price)
 * Rastreamento de preço típico ponderado, com reset por sessão diária.
 */
@org.springframework.stereotype.Component
public class VwapStrategy implements IndicadorStrategy {

    @Override
    public TipoIndicador getTipoIndicador() {
        return TipoIndicador.VWAP;
    }

    @Override
    public IndicadorState inicializarEstado() {
        return new VwapState();
    }

    @Override
    public double calcular(PrimitiveCandle candle, IndicadorContext context) {
        VwapState state = (VwapState) context.estado();

        // Verifica Turnover de Sessão (Reset Diário UTC)
        long currentDay = Instant.ofEpochSecond(candle.dataAbertura()).atZone(ZoneOffset.UTC).getDayOfYear();
        if (state.lastDay != currentDay) {
            state.reset();
            state.lastDay = currentDay;
        }

        double typicalPrice = (candle.maxima() + candle.minima() + candle.valorFechamento()) / 3.0;
        double volume = candle.volume();

        state.cumTypicalVolume += (typicalPrice * volume);
        state.cumVolume += volume;

        return state.cumVolume == 0.0 ? typicalPrice : (state.cumTypicalVolume / state.cumVolume);
    }

    private static class VwapState implements IndicadorState {
        double cumTypicalVolume = 0.0;
        double cumVolume = 0.0;
        long lastDay = -1;

        @Override
        public void reset() {
            cumTypicalVolume = 0.0;
            cumVolume = 0.0;
        }
    }
}