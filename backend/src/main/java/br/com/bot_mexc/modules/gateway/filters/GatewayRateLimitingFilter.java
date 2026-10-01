package br.com.bot_mexc.modules.gateway.filters;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Order(1)
public class GatewayRateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> standardBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> sensitiveBuckets = new ConcurrentHashMap<>();

    private static final int STANDARD_CAPACITY_PER_MINUTE = 120;
    private static final int SENSITIVE_CAPACITY_PER_MINUTE = 20;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String path = request.getRequestURI();

        // Não aplica rate limiting em conexões WebSocket / SSE ativas
        if (path.startsWith("/ws-bot") || path.startsWith("/v1/gateway/stream")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String clientIp = extractClientIp(request);
        final boolean isSensitive = isSensitiveRoute(path);

        // 1. Verificação via Redis (se configurado/disponível)
        if (redisTemplate != null) {
            try {
                String redisKey = "rate_limit:" + clientIp + ":" + (isSensitive ? "strict" : "std");
                Long count = redisTemplate.opsForValue().increment(redisKey);
                if (count != null && count == 1) {
                    redisTemplate.expire(redisKey, 1, TimeUnit.MINUTES);
                }
                int limit = isSensitive ? SENSITIVE_CAPACITY_PER_MINUTE : STANDARD_CAPACITY_PER_MINUTE;
                if (count != null && count > limit) {
                    log.warn("[GATEWAY REDIS 429] Rate limit excedido no Redis para IP: {} na rota: {}", clientIp, path);
                    rejectTooManyRequests(response);
                    return;
                }
            } catch (Exception e) {
                log.debug("Redis rate limiting indisponível, usando fallback local Bucket4j: {}", e.getMessage());
            }
        }

        // 2. Token Bucket local (Bucket4j - ultra-baixo jitter e alta performance)
        Bucket bucket = isSensitive
                ? sensitiveBuckets.computeIfAbsent(clientIp, this::createSensitiveBucket)
                : standardBuckets.computeIfAbsent(clientIp, this::createStandardBucket);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("[GATEWAY 429] Rate limit excedido para IP: {} na rota: {}", clientIp, path);
            rejectTooManyRequests(response);
        }
    }

    private boolean isSensitiveRoute(String path) {
        return path != null && (path.contains("/auth/") || path.contains("/simulac"));
    }

    private void rejectTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
            {
                "status": 429,
                "error": "Too Many Requests",
                "message": "Limite de requisições excedido. Tente novamente em alguns segundos."
            }
        """);
    }

    private Bucket createStandardBucket(String ip) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(STANDARD_CAPACITY_PER_MINUTE)
                .refillGreedy(STANDARD_CAPACITY_PER_MINUTE, Duration.ofMinutes(1))
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private Bucket createSensitiveBucket(String ip) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(SENSITIVE_CAPACITY_PER_MINUTE)
                .refillGreedy(SENSITIVE_CAPACITY_PER_MINUTE, Duration.ofMinutes(1))
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
