package br.com.bot_mexc.models.dtos;

import java.math.BigDecimal;
import java.time.Instant;

public record CompraDTO(
        String id,
        Instant dataCompra,
        BigDecimal valorOperacao,
        BigDecimal valorMoeda,
        BigDecimal volume,
        String snapshotIndicadores
) {
}