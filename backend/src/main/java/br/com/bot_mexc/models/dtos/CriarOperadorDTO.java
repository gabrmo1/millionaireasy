package br.com.bot_mexc.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarOperadorDTO(

        @NotBlank
        @Size(max = 50)
        String nome,

        @NotBlank
        @Size(max = 64)
        String accessKey,

        @NotBlank
        @Size(max = 64)
        String secretKey

) {
}