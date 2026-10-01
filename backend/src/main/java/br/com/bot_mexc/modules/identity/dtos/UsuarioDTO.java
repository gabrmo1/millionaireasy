package br.com.bot_mexc.modules.identity.dtos;
import br.com.bot_mexc.shared.enums.*;
import br.com.bot_mexc.shared.entities.BaseEntity;
import br.com.bot_mexc.modules.identity.entities.*;
import br.com.bot_mexc.modules.identity.dtos.*;
import br.com.bot_mexc.modules.identity.repositories.*;
import br.com.bot_mexc.modules.identity.services.*;
import br.com.bot_mexc.modules.identity.configs.*;

import br.com.bot_mexc.modules.identity.entities.Usuario;
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