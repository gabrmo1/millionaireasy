package br.com.bot_mexc.services;

import br.com.bot_mexc.models.dtos.CandleDTO;
import br.com.bot_mexc.models.entities.Candle;
import br.com.bot_mexc.repositories.CandleRepository;
import br.com.bot_mexc.utils.CandleUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandleService {

    private final CandleRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CURRENT_CANDLE_PREFIX = "mexc:candle:current:";

    @Async("asyncExecutor")
    public void salvarCandlesAsync(List<CandleDTO> candlesDto, String par, String intervalo) {
        final var optionalUltimoCandle = repository.findTopByParAndIntervaloOrderByDataFechamentoDesc(par, intervalo);
        List<Candle> candlesEntity;

        if (optionalUltimoCandle.isPresent()) {
            final var ultimoCandle = optionalUltimoCandle.get();

            if (candlesDto.size() >= 2) {
                final var penultimoCandleDTO = candlesDto.get(candlesDto.size() - 2);
                final var penultimoCandleOptional = repository.findByParAndIntervaloAndDataFechamento(par, intervalo, penultimoCandleDTO.closeTime());

                candlesDto.stream()
                        .filter(c -> c.closeTime().equals(ultimoCandle.getDataFechamento()))
                        .findFirst()
                        .ifPresent(candleAberto -> {
                            ultimoCandle.setVolume(candleAberto.volume());
                            repository.save(ultimoCandle);
                        });

                if (penultimoCandleOptional.isPresent()) {
                    final var penultimoCandle = penultimoCandleOptional.get();

                    if (!penultimoCandle.getVolume().equals(penultimoCandleDTO.volume())) {
                        penultimoCandle.setVolume(penultimoCandleDTO.volume());
                        repository.save(penultimoCandle);
                    }
                }
            }

            candlesEntity = candlesDto.stream()
                    .filter(c -> c.closeTime().isAfter(ultimoCandle.getDataFechamento()))
                    .map(c -> CandleUtils.converterDtoParaEntidade(c, par, intervalo))
                    .toList();

        } else {
            candlesEntity = candlesDto.stream()
                    .map(c -> CandleUtils.converterDtoParaEntidade(c, par, intervalo))
                    .toList();
        }

        if (!candlesEntity.isEmpty())
            repository.saveAll(candlesEntity);
    }

    @Async("asyncExecutor")
    public void salvarCandleWebsocket(CandleDTO novoCandle, String par, String intervalo) {
        final var key = CURRENT_CANDLE_PREFIX + par + ":" + intervalo;

        try {
            final var cachedObj = redisTemplate.opsForValue().get(key);
            CandleDTO candleCacheado = null;

            if (cachedObj != null) {
                candleCacheado = objectMapper.convertValue(cachedObj, CandleDTO.class);
            }

            if (candleCacheado != null && novoCandle.closeTime().isAfter(candleCacheado.closeTime())) {
                log.debug("Fechamento de candle detectado para {}/{}. Persistindo no banco.", par, intervalo);
                salvarNoBancoDireto(candleCacheado, par, intervalo);
            }

            redisTemplate.opsForValue().set(key, novoCandle, 60, TimeUnit.MINUTES);

        } catch (Exception e) {
            log.error("Erro no processamento do candle via Redis (Write-Behind): {}", e.getMessage());
        }
    }

    private void salvarNoBancoDireto(CandleDTO dto, String par, String intervalo) {
        try {
            if (!repository.existsByParAndIntervaloAndDataFechamento(par, intervalo, dto.closeTime()))
                repository.save(CandleUtils.converterDtoParaEntidade(dto, par, intervalo));
        } catch (Exception e) {
            log.error("Falha ao salvar candle fechado no banco: {}", e.getMessage());
        }
    }
}