package br.com.bot_mexc.models.dtos.monitoramento;

import java.math.BigDecimal;
import java.time.Instant;

public record RelatorioDesempenhoDTO(
        Instant periodoInicio,
        Instant periodoFim,
        Integer totalTrades,
        Integer tradesVencedores,
        Integer tradesPerdedores,
        Integer operacoesZeradas,
        BigDecimal lucroBruto,
        BigDecimal prejuizoBruto,
        BigDecimal lucroLiquido,
        BigDecimal fatorLucro,
        BigDecimal maxDrawdownPercentual,
        BigDecimal maxDrawdownNominal,
        BigDecimal maeMedioPercentual,
        BigDecimal maiorVitoria,
        BigDecimal maiorDerrota,
        String tempoMedioOperacao, // Representado em ISO-8601 (ex: PT1H30M) ou formatado
        Integer maiorSequenciaVencedora,
        Integer maiorSequenciaPerdedora
) {}