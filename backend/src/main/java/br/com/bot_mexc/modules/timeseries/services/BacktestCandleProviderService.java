package br.com.bot_mexc.modules.timeseries.services;
import br.com.bot_mexc.shared.enums.*;
import br.com.bot_mexc.shared.utils.DateUtils;
import br.com.bot_mexc.shared.entities.BaseEntity;
import br.com.bot_mexc.shared.configs.RabbitMQConfig;
import br.com.bot_mexc.modules.timeseries.entities.*;
import br.com.bot_mexc.modules.timeseries.dtos.*;
import br.com.bot_mexc.modules.timeseries.repositories.*;
import br.com.bot_mexc.modules.timeseries.services.*;
import br.com.bot_mexc.modules.timeseries.utils.*;
import br.com.bot_mexc.modules.timeseries.builders.*;
import br.com.bot_mexc.modules.strategy.entities.IndicadorConfig;

import br.com.bot_mexc.shared.configs.RabbitMQConfig;
import br.com.bot_mexc.modules.market.integrations.MexcIntegration;
import br.com.bot_mexc.modules.timeseries.dtos.CandleDTO;
import br.com.bot_mexc.modules.timeseries.dtos.CandlePersistPayloadDTO;
import br.com.bot_mexc.modules.timeseries.repositories.CandleRepository;
import br.com.bot_mexc.modules.timeseries.utils.CandleUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class BacktestCandleProviderService {

    private final CandleRepository candleRepository;
    private final MexcIntegration mexcIntegration;
    private final RabbitTemplate rabbitTemplate;

    public List<CandleDTO> obterCandles(String par, String intervalo, Instant dataInicio, Instant dataFim) {
        long inicioMs = dataInicio.toEpochMilli();
        long fimMs = dataFim.toEpochMilli();
        long intervalMs = parseIntervalToMillis(intervalo);
        String restInterval = parseIntervalToRestFormat(intervalo);

        // 1. Ingestion Local
        final var dbCandles = candleRepository.findByParAndIntervaloAndDataAberturaBetweenOrderByDataAberturaAsc(par, intervalo, dataInicio, dataFim);

        // Load Factor 1.0 evita rehash em memória.
        Set<Long> dbTimestamps = new HashSet<>(dbCandles.size(), 1.0f);
        List<CandleDTO> result = new ArrayList<>(dbCandles.size());

        for (var c : dbCandles) {
            dbTimestamps.add(c.getDataAbertura().toEpochMilli());
            result.add(CandleUtils.buildDtoFromEntity(c));
        }

        // 2. Identificação de Gaps (O(N) contíguo)
        List<long[]> gaps = new ArrayList<>();
        long gapStart = -1;

        for (long t = inicioMs; t <= fimMs; t += intervalMs) {
            if (!dbTimestamps.contains(t)) {
                if (gapStart == -1) gapStart = t;
            } else {
                if (gapStart != -1) {
                    gaps.add(new long[]{gapStart, t - intervalMs});
                    gapStart = -1;
                }
            }
        }

        if (gapStart != -1) {
            gaps.add(new long[]{gapStart, fimMs});
        }

        // 3. Fetching e Assynchronous Write-Behind
        List<CandleDTO> fetchedToPersist = new ArrayList<>();

        for (long[] gap : gaps) {
            long chunkStart = gap[0];
            long gapEnd = gap[1];

            while (chunkStart <= gapEnd) {
                // Paginação limitando a 2000 candles por request (Restrição MEXC API V3)
                long chunkEnd = Math.min(chunkStart + (2000L * intervalMs) - intervalMs, gapEnd);

                // [VORTEX CORE] Consulta executada no formato estrito (ex: 60m) evadindo HTTP 400 (-1121)
                String response = mexcIntegration.obterCandles(par, restInterval, "2000", chunkStart, chunkEnd);
                List<CandleDTO> fetched = CandleUtils.buildListCandleDtoFromMexcResponse(response);

                fetchedToPersist.addAll(fetched);
                result.addAll(fetched);

                chunkStart = chunkEnd + intervalMs;
            }
        }

        // Fire-and-Forget via AMQP (Isolamento de Falha)
        if (!fetchedToPersist.isEmpty()) {
            CandlePersistPayloadDTO payload = new CandlePersistPayloadDTO(par, intervalo, fetchedToPersist);
            rabbitTemplate.convertAndSend(RabbitMQConfig.CANDLES_PERSIST_QUEUE, payload);
            log.info("[BACKTEST] {} candles históricos de {}/{} recuperados da API e enviados para persistência assíncrona.", fetchedToPersist.size(), par, intervalo);
        }

        // 4. Unificação para o Motor de Simulação
        result.sort(Comparator.comparingLong(CandleDTO::dataAbertura));

        return result;
    }

    private long parseIntervalToMillis(String interval) {
        return switch (interval) {
            case "1m", "Min1" -> 60_000L;
            case "5m", "Min5" -> 300_000L;
            case "15m", "Min15" -> 900_000L;
            case "30m", "Min30" -> 1_800_000L;
            case "60m", "1h", "Min60" -> 3_600_000L;
            case "4h", "Hour4" -> 14_400_000L;
            case "8h", "Hour8" -> 28_800_000L;
            case "1d", "Day1" -> 86_400_000L;
            default -> throw new IllegalArgumentException("Intervalo não suportado: " + interval);
        };
    }

    private String parseIntervalToRestFormat(String interval) {
        return switch (interval) {
            case "1h", "Min60" -> "60m";
            case "Hour4" -> "4h";
            case "Hour8" -> "8h";
            case "Day1" -> "1d";
            case "Min1" -> "1m";
            case "Min5" -> "5m";
            case "Min15" -> "15m";
            case "Min30" -> "30m";
            case "Month1" -> "1M";
            default -> interval; // Retorna como recebido caso já esteja em formato REST válido (1m, 5m, 15m, 30m, 60m)
        };
    }
}