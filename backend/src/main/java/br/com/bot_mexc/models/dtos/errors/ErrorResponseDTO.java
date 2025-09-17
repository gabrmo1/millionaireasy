package br.com.bot_mexc.models.dtos.errors;

import java.time.LocalDateTime;

public record ErrorResponseDTO(
        String message,
        LocalDateTime timestamp
) {
}