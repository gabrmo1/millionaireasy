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
 * RSI Estocástico (StochRSI)
 * Construído sobre Média Suavizada de Wilder e Média Móvel Simples (SMA)[cite: 3].
 * Isolamento total na heap (O(1) Memory footprint).
 */
@org.springframework.stereotype.Component
public class RsiEstocasticoStrategy implements IndicadorStrategy {

    @Override
    public TipoIndicador getTipoIndicador() {
        return TipoIndicador.RSI_ESTOCASTICO_K;
    }

    @Override
    public IndicadorState inicializarEstado() {
        return new StochRsiState();
    }

    @Override
    public double calcular(PrimitiveCandle candle, IndicadorContext context) {
        StochRsiState state = (StochRsiState) context.estado();

        int rsiPeriod = context.parametros().getOrDefault("periodo", 14);
        int smoothK = context.parametros().getOrDefault("suavizacaoK", 3);
        int smoothD = context.parametros().getOrDefault("suavizacaoD", 3);
        int returnType = context.parametros().getOrDefault("retorno", 0); // 0 = Linha K, 1 = Linha D

        if (state.windowRsi == null) {
            state.windowRsi = new double[rsiPeriod];
            state.kRawBuffer = new PrimitiveRingBuffer(smoothK);
            state.kBuffer = new PrimitiveRingBuffer(smoothD);
        }

        // 1. Wilder's Smoothing para o RSI subjacente[cite: 3]
        double change = candle.valorFechamento() - context.precoAnterior();
        double gain = change > 0 ? change : 0;
        double loss = change < 0 ? -change : 0;

        if (Double.isNaN(state.avgGain)) {
            state.avgGain = gain;
            state.avgLoss = loss;
        } else {
            state.avgGain = ((state.avgGain * (rsiPeriod - 1)) + gain) / rsiPeriod;
            state.avgLoss = ((state.avgLoss * (rsiPeriod - 1)) + loss) / rsiPeriod;
        }

        // Divisão segura: Se perda for 0, RSI forçado a 100 para evitar ArithmeticException[cite: 3]
        double rsi = (state.avgLoss == 0.0) ? 100.0 : 100.0 - (100.0 / (1.0 + (state.avgGain / state.avgLoss)));

        // Gerenciamento de Janela Circular customizado para Min/Max
        state.windowRsi[state.idxRsi] = rsi;
        state.idxRsi = (state.idxRsi + 1) % rsiPeriod;
        if (state.countRsi < rsiPeriod) state.countRsi++;

        if (state.countRsi < rsiPeriod) return Double.NaN; // Ignora warmup inicial

        // 2. Busca Min e Max do RSI na janela temporal atual
        double minRsi = Double.MAX_VALUE;
        double maxRsi = Double.MIN_VALUE;
        for (int i = 0; i < state.countRsi; i++) {
            if (state.windowRsi[i] < minRsi) minRsi = state.windowRsi[i];
            if (state.windowRsi[i] > maxRsi) maxRsi = state.windowRsi[i];
        }

        // 3. Calcula o Stoch bruto e extrai as linhas K e D (usando SMAs)[cite: 3]
        double stochBruto = (maxRsi == minRsi) ? 0.0 : ((rsi - minRsi) / (maxRsi - minRsi)) * 100.0;

        state.kRawBuffer.add(stochBruto);
        double linhaK = state.kRawBuffer.getAverage();

        state.kBuffer.add(linhaK);
        double linhaD = state.kBuffer.getAverage();

        return returnType == 0 ? linhaK : linhaD;
    }

    private static class StochRsiState implements IndicadorState {
        double avgGain = Double.NaN;
        double avgLoss = Double.NaN;

        double[] windowRsi;
        int idxRsi = 0;
        int countRsi = 0;

        PrimitiveRingBuffer kRawBuffer;
        PrimitiveRingBuffer kBuffer;

        @Override
        public void reset() {
            avgGain = Double.NaN;
            avgLoss = Double.NaN;
            idxRsi = 0;
            countRsi = 0;
            if (kRawBuffer != null) kRawBuffer.reset();
            if (kBuffer != null) kBuffer.reset();
        }
    }
}