package br.com.bot_mexc.models.dtos.mexc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SymbolInfoDTO {
    private String symbol;
    private String status;
    private String baseAsset;
    private String quoteAsset;
}