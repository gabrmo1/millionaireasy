package br.com.bot_mexc.modules.gateway.dtos.errors;

import java.time.Instant;

public record ErrorResponseDTO(
        String message,
        Instant timestamp
) {
}