package br.com.bot_mexc.models.dtos;

import br.com.bot_mexc.models.enums.StatusOperacoes;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record OperacaoDTO(
        String id,
        StatusOperacoes status,
        Instant dataCriacao,
        Instant dataInicio,
        Instant dataFim,
        String par,
        String intervalo,
        OperadorDTO operador,
        EstrategiaDTO estrategia,
        Boolean modoTeste,
        BigDecimal saldoInicial
) {
}