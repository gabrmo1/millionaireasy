package br.com.bot_mexc.configs.hibernate;

import br.com.bot_mexc.models.entities.Usuario;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TenantFilterAspect {

    private final EntityManager entityManager;

    public TenantFilterAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Before("execution(* org.springframework.data.jpa.repository.JpaRepository.*(..))")
    public void activateUserFilter() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            Usuario user = (Usuario) authentication.getPrincipal();
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("userFilter").setParameter("userId", user.getId());
        }
    }

}