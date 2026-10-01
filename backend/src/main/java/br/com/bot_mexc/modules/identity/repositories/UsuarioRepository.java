package br.com.bot_mexc.modules.identity.repositories;
import br.com.bot_mexc.shared.enums.*;
import br.com.bot_mexc.shared.entities.BaseEntity;
import br.com.bot_mexc.modules.identity.entities.*;
import br.com.bot_mexc.modules.identity.dtos.*;
import br.com.bot_mexc.modules.identity.repositories.*;
import br.com.bot_mexc.modules.identity.services.*;
import br.com.bot_mexc.modules.identity.configs.*;

import br.com.bot_mexc.modules.identity.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, String> {
    Optional<Usuario> findByEmail(String email);
}