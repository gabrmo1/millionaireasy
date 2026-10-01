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


import br.com.bot_mexc.modules.timeseries.dtos.CandleDTO;
import br.com.bot_mexc.modules.strategy.dtos.PrimitiveCandle;
import br.com.bot_mexc.shared.enums.TipoIndicador;

/**
 * Contrato principal do motor de cálculo baseado no padrão Strategy.
 * Substitui o acoplamento do CalculoUtils e as inner classes do IndicadorTracker.
 */
public interface IndicadorStrategy {

    /**
     * Identificador do indicador que esta estratégia processa.
     */
    TipoIndicador getTipoIndicador();

    /**
     * Calcula o valor do indicador no tick atual.
     * Operações aritméticas intensivas devem ocorrer em primitivos (double)
     * e o estado subjacente atualizado in-place no contexto.
     *
     * @param candle Dados do tick atual
     * @param context Contexto contendo estado mutável L1 e parâmetros
     * @return Valor calculado em primitivo (double) para evitar GC pressure
     */
    double calcular(PrimitiveCandle candle, IndicadorContext context);

    /**
     * Fabrica o estado inicial pré-alocado na RAM.
     */
    IndicadorState inicializarEstado();
}