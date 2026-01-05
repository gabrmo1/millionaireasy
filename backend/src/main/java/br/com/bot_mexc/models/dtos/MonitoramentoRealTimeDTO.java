package br.com.bot_mexc.models.dtos;

import java.math.BigDecimal;
import java.util.Map;

public record MonitoramentoRealTimeDTO(
        CandleDTO candle,
        Map<String, BigDecimal> indicadores
) {}