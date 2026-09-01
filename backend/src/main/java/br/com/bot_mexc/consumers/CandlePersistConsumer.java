package br.com.bot_mexc.consumers;

import br.com.bot_mexc.configs.RabbitMQConfig;
import br.com.bot_mexc.models.dtos.CandlePersistPayloadDTO;
import br.com.bot_mexc.models.entities.Candle;
import br.com.bot_mexc.repositories.CandleRepository;
import br.com.bot_mexc.utils.CandleUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CandlePersistConsumer {

    private final CandleRepository candleRepository;

    @Transactional
    @RabbitListener(queues = RabbitMQConfig.CANDLES_PERSIST_QUEUE)
    public void processarLoteCandles(CandlePersistPayloadDTO payload) {
        if (payload == null || payload.candles() == null || payload.candles().isEmpty()) {
            return;
        }

        final long inicioMs = System.currentTimeMillis();
        final String par = payload.par();
        final String intervalo = payload.intervalo();

        // Mapeamento O(N) linear via Streams com alocação otimizada
        List<Candle> entidades = payload.candles().stream()
                .map(dto -> CandleUtils.buildEntityFromDto(dto, par, intervalo))
                .toList();

        // Batch insert: Otimização de I/O de disco e rede
        candleRepository.saveAll(entidades);

        final long tempoProcessamento = System.currentTimeMillis() - inicioMs;
        log.info("[PERSISTÊNCIA] Lote de {} candles de {}/{} persistido em {} ms.",
                entidades.size(), par, intervalo, tempoProcessamento);
    }
}