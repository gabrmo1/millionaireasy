package br.com.bot_mexc.models.entities;

import br.com.bot_mexc.utils.DateUtils;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

@Data
@MappedSuperclass
@FilterDef(name = "userFilter", parameters = {@ParamDef(name = "userId", type = String.class)})
@Filter(name = "userFilter", condition = "id_usuario = :userId")
public class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", unique = true, nullable = false, updatable = false, length = 36)
    String id;

    @Column(name = "data_criacao")
    LocalDateTime dataCriacao;

    @Column(name = "id_usuario", nullable = false, updatable = false, length = 36)
    String idUsuario;

    @PrePersist
    public void prePersist() {
        if (this.dataCriacao == null) dataCriacao = DateUtils.agora();

        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            Usuario user = (Usuario) authentication.getPrincipal();
            this.idUsuario = user.getId();
        } else {
            this.idUsuario = "SYSTEM";
        }
    }

}