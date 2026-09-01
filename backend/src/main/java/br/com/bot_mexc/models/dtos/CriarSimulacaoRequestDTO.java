package br.com.bot_mexc.models.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record CriarSimulacaoRequestDTO(
        @NotBlank @Size(max = 20) String par,
        @NotBlank @Size(max = 5) String intervalo,
        @NotBlank String idEstrategia,
        @NotNull @Positive BigDecimal saldoInicial,

        @NotNull
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant dataInicio,

        @NotNull
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant dataFim
) {
    public CriarSimulacaoRequestDTO {
        if (dataInicio != null && dataFim != null && dataInicio.isAfter(dataFim)) {
            throw new IllegalArgumentException("A dataInicio não pode ser posterior a dataFim");
        }
    }
}