package br.com.bot_mexc.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CriarOperacaoDTO(

        @NotBlank
        @Size(max = 20)
        String par,

        @NotBlank
        @Size(max = 5)
        String intervalo,

        String idOperador,

        String idEstrategia,

        Boolean modoTeste,

        @Positive
        BigDecimal saldoInicial

) {
}