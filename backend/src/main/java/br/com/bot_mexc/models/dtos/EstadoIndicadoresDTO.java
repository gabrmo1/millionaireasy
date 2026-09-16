package br.com.bot_mexc.models.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.With;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Estado atualizado para suportar retrocompatibilidade com o banco ativo (L2 Cache / Redis)
 * e estender propriedades para novos indicadores se necessário.
 */
@Builder
@With
@JsonIgnoreProperties(ignoreUnknown = true)
public record EstadoIndicadoresDTO(
        Long ultimaDataFechamento,
        BigDecimal ultimoPrecoFechamento,
        Map<String, EstadoIndicadorItem> estados
) {
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EstadoIndicadorItem(
            BigDecimal valor,
            BigDecimal avgGain, // Retrocompatibilidade (RSI legado)
            BigDecimal avgLoss, // Retrocompatibilidade (RSI legado)
            BigDecimal soma,    // Retrocompatibilidade (SMA legado)
            Map<String, Double> metadadosL2 // Suporte para extensões futuras L2
    ) {}
}