package br.com.bot_mexc.services.strategy;

import br.com.bot_mexc.models.dtos.CandleDTO;
import br.com.bot_mexc.models.dtos.PrimitiveCandle;
import br.com.bot_mexc.models.enums.TipoIndicador;

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