package br.com.bot_mexc.models.dtos.mexc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SymbolInfoDTO implements Serializable {
    private String symbol;
    private String status;
    private String baseAsset;
    private String quoteAsset;
}