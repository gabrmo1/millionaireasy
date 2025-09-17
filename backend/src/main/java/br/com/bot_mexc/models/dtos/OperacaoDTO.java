package br.com.bot_mexc.models.dtos;

import br.com.bot_mexc.models.enums.StatusOperacoes;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record OperacaoDTO(
        String id,
        StatusOperacoes status,
        LocalDateTime dataCriacao,
        LocalDateTime dataInicio,
        LocalDateTime dataFim,
        String par,
        String intervalo,
        OperadorDTO operador,
        EstrategiaDTO estrategia
) {
}