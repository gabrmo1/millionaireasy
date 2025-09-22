package br.com.bot_mexc.models.dtos.mexc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExchangeInfoDTO {
    private List<SymbolInfoDTO> symbols;
}