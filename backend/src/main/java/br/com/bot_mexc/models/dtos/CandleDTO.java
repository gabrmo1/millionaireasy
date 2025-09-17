package br.com.bot_mexc.models.dtos;

import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@With
public record CandleDTO(

        LocalDateTime openTime,

        LocalDateTime closeTime,

        BigDecimal openValue,

        BigDecimal closeValue,

        BigDecimal low,

        BigDecimal high,

        BigDecimal volume

) {
}
