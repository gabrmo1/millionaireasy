package br.com.bot_mexc.models.dtos;

import java.math.BigDecimal;

public record ConsultarRsiResponseDTO(

        BigDecimal d,

        BigDecimal dAnterior,

        BigDecimal k,

        BigDecimal kAnterior,

        BigDecimal rsiCurto,

        BigDecimal rsiMedio,

        BigDecimal rsiLongo,

        BigDecimal valorAtualMoeda

) {
}
