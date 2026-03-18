package br.com.bot_mexc.models.dtos;

import java.math.BigDecimal;
import java.time.Instant;

public record VendaDTO(
        String id,
        Instant dataVenda,
        BigDecimal valorCompra,
        BigDecimal valorVenda,
        BigDecimal lucro,
        String snapshotIndicadores
) {
}