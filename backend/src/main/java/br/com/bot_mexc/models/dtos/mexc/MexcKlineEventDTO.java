package br.com.bot_mexc.models.dtos.mexc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MexcKlineEventDTO(

        @JsonProperty("symbol")
        String symbol,

        @JsonProperty("interval")
        String interval,

        @JsonProperty("windowStart")
        Long windowStart,

        @JsonProperty("windowEnd")
        Long windowEnd,

        @JsonProperty("openingPrice")
        BigDecimal open,

        @JsonProperty("closingPrice")
        BigDecimal close,

        @JsonProperty("highestPrice")
        BigDecimal high,

        @JsonProperty("lowestPrice")
        BigDecimal low,

        @JsonProperty("volume")
        BigDecimal volume,

        @JsonProperty("amount")
        BigDecimal amount

) {
}