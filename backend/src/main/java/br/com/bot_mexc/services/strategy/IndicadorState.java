package br.com.bot_mexc.services.strategy;

/**
 * Interface marcadora para o estado em cache dos indicadores.
 * OBRIGATÓRIO: Implementações devem utilizar tipos primitivos mutáveis
 * para garantir Zero Allocation no Heap durante a iteração do WebSocket/Backtest.
 */
public interface IndicadorState {
    void reset();
}