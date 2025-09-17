package br.com.bot_mexc.models.dtos;

import java.math.BigDecimal;

public record ValorMoedaDTO(

        String symbol,

        BigDecimal price

) {
}
