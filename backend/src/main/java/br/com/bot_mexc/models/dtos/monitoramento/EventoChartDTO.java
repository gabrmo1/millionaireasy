package br.com.bot_mexc.models.dtos.monitoramento;

import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record EventoChartDTO(
        long time,
        String tipo,
        BigDecimal preco,
        String tooltip,
        String cor
) {}