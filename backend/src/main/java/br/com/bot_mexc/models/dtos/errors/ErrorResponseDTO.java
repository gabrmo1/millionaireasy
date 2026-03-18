package br.com.bot_mexc.models.dtos.errors;

import java.time.Instant;

public record ErrorResponseDTO(
        String message,
        Instant timestamp
) {
}