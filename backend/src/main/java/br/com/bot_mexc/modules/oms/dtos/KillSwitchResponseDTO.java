package br.com.bot_mexc.modules.oms.dtos;

import java.time.Instant;

public record KillSwitchResponseDTO(
        int operacoesParadas,
        String status,
        String mensagem,
        Instant executadoEm
) {}
