package br.com.bot_mexc.models.dtos;

import lombok.Builder;

@Builder
public record OperadorDTO(

        String id,

        String nome,

        String accessKey,

        String secretKey

) {
}
