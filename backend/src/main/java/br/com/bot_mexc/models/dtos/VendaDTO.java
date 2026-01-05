package br.com.bot_mexc.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VendaDTO(
        String id,
        LocalDateTime dataVenda,
        BigDecimal valorCompra,
        BigDecimal valorVenda,
        BigDecimal lucro,
        String snapshotIndicadores
) {
}