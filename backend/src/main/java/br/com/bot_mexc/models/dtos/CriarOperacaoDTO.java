package br.com.bot_mexc.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CriarOperacaoDTO(

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