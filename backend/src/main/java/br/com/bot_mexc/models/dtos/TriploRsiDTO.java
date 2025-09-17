package br.com.bot_mexc.models.dtos;

import java.math.BigDecimal;
import java.util.List;

public record TriploRsiDTO(

        BigDecimal rsiCurto,

        BigDecimal rsiMedio,

        BigDecimal rsiLongo,

        List<BigDecimal> rsiMedioSerie

) {
}
