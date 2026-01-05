package br.com.bot_mexc.models.dtos.monitoramento;

import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record IndicadorPointDTO(
        long time,
        BigDecimal value
) {}