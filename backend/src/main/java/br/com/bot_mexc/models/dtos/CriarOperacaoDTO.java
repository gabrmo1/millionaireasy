package br.com.bot_mexc.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CriarOperacaoDTO(

        @NotNull
        LocalDateTime dataInicio,

        @NotNull
        LocalDateTime dataFim,

        @NotBlank
        @Size(max = 20)
        String par,

        @NotBlank
        @Size(max = 5)
        String intervalo,

        @NotEmpty
        @NotBlank
        String idOperador,

        @NotEmpty
        @NotBlank
        String idEstrategia

) {
}