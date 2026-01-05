package br.com.bot_mexc.models.dtos.monitoramento;

import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record CandleChartDTO(
        long time,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal volume
) {}