
package br.com.bot_mexc.models.dtos;

import lombok.Builder;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Builder
@With
public record EstadoIndicadoresDTO(

        LocalDateTime ultimaDataFechamento,

        BigDecimal ultimoPrecoFechamento,

        Map<String, EstadoIndicadorItem> estados
) {
    @Builder
    public record EstadoIndicadorItem(
            BigDecimal valor,
            BigDecimal avgGain,
            BigDecimal avgLoss,
            BigDecimal soma
    ) {
    }
}