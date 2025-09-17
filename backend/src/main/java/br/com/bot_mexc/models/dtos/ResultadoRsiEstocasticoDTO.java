package br.com.bot_mexc.models.dtos;

import java.math.BigDecimal;

public record ResultadoRsiEstocasticoDTO(

        BigDecimal k,

        BigDecimal kAnterior,

        BigDecimal d,

        BigDecimal dAnterior

) {
}
