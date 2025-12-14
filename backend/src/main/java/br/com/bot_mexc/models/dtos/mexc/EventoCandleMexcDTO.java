package br.com.bot_mexc.models.dtos.mexc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventoCandleMexcDTO(

        @JsonProperty("symbol")
        String par,

        @JsonProperty("interval")
        String intervalo,

        @JsonProperty("windowStart")
        Long inicioJanela,

        @JsonProperty("windowEnd")
        Long fimJanela,

        @JsonProperty("openingPrice")
        BigDecimal precoAbertura,

        @JsonProperty("closingPrice")
        BigDecimal precoFechamento,

        @JsonProperty("highestPrice")
        BigDecimal maxima,

        @JsonProperty("lowestPrice")
        BigDecimal minima,

        @JsonProperty("volume")
        BigDecimal volume,

        @JsonProperty("amount")
        BigDecimal valorTotal
) {
}