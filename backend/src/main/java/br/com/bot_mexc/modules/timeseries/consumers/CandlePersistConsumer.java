package br.com.bot_mexc.modules.timeseries.consumers;

import br.com.bot_mexc.modules.timeseries.dtos.CandlePersistPayloadDTO;
import br.com.bot_mexc.modules.timeseries.services.CandleJdbcBatchService;
import br.com.bot_mexc.shared.configs.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CandlePersistConsumer {

    private final CandleJdbcBatchService candleBatchService;

    @Transactional
    @RabbitListener(queues = RabbitMQConfig.CANDLES_PERSIST_QUEUE)
    public void processarLoteCandles(CandlePersistPayloadDTO payload) {
        if (payload == null || payload.candles() == null || payload.candles().isEmpty()) {
            return;
        }

        candleBatchService.batchInsertCandles(payload.candles(), payload.par(), payload.intervalo());
    }
}