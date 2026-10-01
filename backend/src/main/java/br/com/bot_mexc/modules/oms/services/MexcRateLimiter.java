package br.com.bot_mexc.modules.oms.services;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
public class MexcRateLimiter {

    // Limite padrão seguro: 20 requisições por segundo com recarga contínua (greedy)
    private final Bucket bucket;

    public MexcRateLimiter() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(20)
                .refillGreedy(20, Duration.ofSeconds(1))
                .build();

        this.bucket = Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public boolean tryAcquire() {
        return bucket.tryConsume(1);
    }

    public void acquireToken() {
        try {
            bucket.asBlocking().consume(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[RATE-LIMIT] Thread interrompida ao aguardar token de quota MEXC: {}", e.getMessage());
        }
    }
}
