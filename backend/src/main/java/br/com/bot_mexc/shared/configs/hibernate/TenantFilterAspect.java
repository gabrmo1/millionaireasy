package br.com.bot_mexc.shared.configs.hibernate;

import br.com.bot_mexc.shared.configs.annotations.IgnoreTenantFilter;
import br.com.bot_mexc.modules.identity.entities.Usuario;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.hibernate.Session;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class TenantFilterAspect {

    private final EntityManager entityManager;

    public TenantFilterAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Before("execution(* br.com.bot_mexc.repositories..*.*(..))")
    public void handleUserFilter(JoinPoint joinPoint) {
        Session session = entityManager.unwrap(Session.class);

        if (shouldIgnoreFilter(joinPoint)) {
            session.disableFilter("userFilter");
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            Usuario user = (Usuario) authentication.getPrincipal();
            session.enableFilter("userFilter").setParameter("userId", user.getId());
        }
    }

    private boolean shouldIgnoreFilter(JoinPoint joinPoint) {
        if (!(joinPoint.getSignature() instanceof MethodSignature signature)) {
            return false;
        }

        Method method = signature.getMethod();
        return method.isAnnotationPresent(IgnoreTenantFilter.class)
                || joinPoint.getTarget().getClass().isAnnotationPresent(IgnoreTenantFilter.class);
    }
}