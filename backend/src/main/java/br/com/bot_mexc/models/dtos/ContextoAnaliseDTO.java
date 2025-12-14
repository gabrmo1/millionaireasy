package br.com.bot_mexc.models.dtos;

import br.com.bot_mexc.models.entities.IndicadorConfig;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record ContextoAnaliseDTO(
        CandleDTO candle,
        List<OperacaoCacheDTO> operacoes,
        boolean isFechamentoDeCandle
) {
    public boolean semOperacoes() {
        return operacoes == null || operacoes.isEmpty();
    }

    public String getPar() {
        return operacoes.getFirst().par();
    }

    public String getIntervalo() {
        return operacoes.getFirst().intervalo();
    }

    public Set<IndicadorConfig> extrairConfiguracoesUnicasDeIndicadores() {
        return operacoes.stream()
                .flatMap(op -> op.indicadores().stream())
                .collect(Collectors.toSet());
    }
}