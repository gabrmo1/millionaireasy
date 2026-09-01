package br.com.bot_mexc.models.dtos;

import br.com.bot_mexc.models.enums.StatusOperacoes;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record SimulacaoDTO(
        String id,
        String par,
        String intervalo,
        StatusOperacoes status,
        EstrategiaDTO estrategia,
        BigDecimal saldoInicial,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant dataCriacao,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant dataInicio,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant dataFim
) {}