package br.com.bot_mexc.services;

import br.com.bot_mexc.models.dtos.CandleDTO;
import br.com.bot_mexc.repositories.CandleRepository;
import br.com.bot_mexc.utils.CandleUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandleService {

    private final CandleRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String PREFIXO_CANDLE_ATUAL = "mexc:candle:current:";

    public Optional<CandleDTO> verificarViradaEAtualizarCache(CandleDTO novoCandle, String par, String intervalo) {
        final var key = PREFIXO_CANDLE_ATUAL + par + ":" + intervalo;

        try {
            final var cachedObj = redisTemplate.opsForValue().get(key);
            var candleCacheado = (CandleDTO) null;

            if (cachedObj != null)
                candleCacheado = objectMapper.convertValue(cachedObj, CandleDTO.class);

            // Se o candle que chegou tem data de fechamento posterior ao que está no cache, significa que o do cache fechou.
            if (candleCacheado != null && novoCandle.dataFechamento() > candleCacheado.dataFechamento()) {
                log.debug("Turnover detectado: Persistindo candle fechado {} para {}/{}.", candleCacheado.dataFechamento(), par, intervalo);

                salvarNoBancoDireto(candleCacheado, par, intervalo);

                // Atualiza o cache com o novo candle que acabou de abrir
                redisTemplate.opsForValue().set(key, novoCandle, 60, TimeUnit.MINUTES);

                return Optional.of(candleCacheado);
            }

            // Não houve virada, apenas atualiza o preço atual no cache
            redisTemplate.opsForValue().set(key, novoCandle, 60, TimeUnit.MINUTES);

            return Optional.empty();

        } catch (Exception e) {
            log.error("Erro no processamento do candle via Redis: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Async("asyncExecutor")
    public void salvarNoBancoDireto(CandleDTO dto, String par, String intervalo) {
        try {
            if (!repository.existsByParAndIntervaloAndDataFechamento(par, intervalo, Instant.ofEpochSecond(dto.dataFechamento())))
                repository.save(CandleUtils.buildEntityFromDto(dto, par, intervalo));
        } catch (Exception e) {
            log.error("Falha ao salvar candle fechado no banco: {}", e.getMessage());
        }
    }
}