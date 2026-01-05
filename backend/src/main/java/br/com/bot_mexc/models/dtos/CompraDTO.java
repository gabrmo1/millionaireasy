package br.com.bot_mexc.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompraDTO(
        String id,
        LocalDateTime dataCompra,
        BigDecimal valorOperacao,
        BigDecimal valorMoeda,
        BigDecimal volume,
        String snapshotIndicadores
) {}