package br.com.bot_mexc.services.strategy;

import java.util.Map;

/**
 * Contexto de execução injetado a cada tick.
 * Utiliza Record nativo do Java 21 para imutabilidade estrutural e pattern matching,
 * encapsulando o estado mutável isolado e buffers primitivos (Mechanical Sympathy).
 */
public record IndicadorContext(
        Map<String, Integer> parametros,
        IndicadorState estado,
        double precoAnterior,
        double[] historicoPrecos // Preparado para o RingBuffer primitivo (Task 2)
) {}