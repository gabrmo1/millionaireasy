package br.com.bot_mexc.security.dtos;

import br.com.bot_mexc.models.entities.Usuario;
import lombok.Builder;

@Builder
public record UsuarioDTO(
        String id,
        String email,
        String role
) {
    public static UsuarioDTO fromEntity(Usuario usuario) {
        return UsuarioDTO.builder()
                .id(usuario.getId())
                .email(usuario.getEmail())
                .role(usuario.getRole().name())
                .build();
    }
}