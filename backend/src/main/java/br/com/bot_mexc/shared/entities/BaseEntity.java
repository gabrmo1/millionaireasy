package br.com.bot_mexc.shared.entities;

import br.com.bot_mexc.shared.utils.DateUtils;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;

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
    Instant dataCriacao;

    @Column(name = "id_usuario", nullable = false, updatable = false, length = 36)
    String idUsuario;

    @PrePersist
    public void prePersist() {
        if (this.dataCriacao == null) dataCriacao = DateUtils.agora();

        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof br.com.bot_mexc.shared.security.UserPrincipal userPrincipal) {
                this.idUsuario = userPrincipal.getId();
            } else {
                this.idUsuario = authentication.getName();
            }
        } else {
            this.idUsuario = "SYSTEM";
        }
    }

}