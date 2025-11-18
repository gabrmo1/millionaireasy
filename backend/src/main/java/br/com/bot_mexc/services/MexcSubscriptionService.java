package br.com.bot_mexc.services;

import br.com.bot_mexc.models.entities.Operacao;
import br.com.bot_mexc.models.enums.StatusOperacoes;
import br.com.bot_mexc.repositories.OperacaoRepository;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class MexcSubscriptionService {

    private static final String KLINE_CHANNEL_PREFIX = "spot@public.kline.v3.api.pb@";
    private static final String REDIS_HASH_KEY = "mexc:channel:refcount";

    private final HashOperations<String, String, String> hashOperations;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MexcWebSocketClient webSocketClient;
    private final OperacaoRepository operacaoRepository;

    public MexcSubscriptionService(@Lazy MexcWebSocketClient webSocketClient,
                                   RedisTemplate<String, Object> redisTemplate,
                                   OperacaoRepository operacaoRepository) {
        this.webSocketClient = webSocketClient;
        this.redisTemplate = redisTemplate;
        this.hashOperations = redisTemplate.opsForHash();
        this.operacaoRepository = operacaoRepository;
    }

    public void addSubscription(String par, String intervalo) {
        final var nomeCanal = formatarNomeCanal(par, traduzirIntervaloParaApi(intervalo));
        try {
            var contagem = hashOperations.increment(REDIS_HASH_KEY, nomeCanal, 1L);
            log.info("Contagem de referência para o canal {} aumentada para {}", nomeCanal, contagem);

            if (contagem == 1) {
                log.info("Primeira inscrição detectada. Solicitando subscrição no WebSocket para {}", nomeCanal);
                webSocketClient.subscribe(nomeCanal);
            }
        } catch (Exception e) {
            log.error("Falha ao incrementar/subscrever canal {} no Redis", nomeCanal, e);
            hashOperations.increment(REDIS_HASH_KEY, nomeCanal, -1L);
        }
    }

    public void removeSubscription(String par, String intervalo) {
        final var nomeCanal = formatarNomeCanal(par, traduzirIntervaloParaApi(intervalo));
        try {
            if (Boolean.FALSE.equals(hashOperations.hasKey(REDIS_HASH_KEY, nomeCanal))) {
                log.warn("Solicitada remoção de inscrição para o canal {}, mas ele não está no Redis.", nomeCanal);
                return;
            }

            var contagem = hashOperations.increment(REDIS_HASH_KEY, nomeCanal, -1L);
            log.info("Contagem de referência para o canal {} reduzida para {}", nomeCanal, contagem);

            if (contagem <= 0) {
                log.info("Última inscrição removida. Solicitando un-subscrição no WebSocket para {}", nomeCanal);
                webSocketClient.unsubscribe(nomeCanal);
                hashOperations.delete(REDIS_HASH_KEY, nomeCanal);
            }
        } catch (Exception e) {
            log.error("Falha ao decrementar/un-subscrever canal {} no Redis", nomeCanal, e);
            hashOperations.increment(REDIS_HASH_KEY, nomeCanal, 1L);
        }
    }

    public void resyncSubscriptionsFromDatabase() {
        log.info("[RESYNC] Iniciando ressincronização das subscrições a partir do banco de dados...");
        List<Operacao> operacoesAtivas = operacaoRepository.findByStatus(StatusOperacoes.EM_ANDAMENTO);

        log.info("[RESYNC] Limpando chave antiga do Redis: {}", REDIS_HASH_KEY);
        redisTemplate.delete(REDIS_HASH_KEY);

        if (operacoesAtivas.isEmpty()) {
            log.info("[RESYNC] Nenhuma operação 'EM_ANDAMENTO' encontrada. Ressincronização concluída (Redis limpo).");
            return;
        }

        log.info("[RESYNC] Repopulando Redis e subscrevendo {} operações ativas...", operacoesAtivas.size());

        for (Operacao op : operacoesAtivas) {
            this.addSubscription(op.getPar(), op.getIntervalo());
        }

        log.info("[RESYNC] Ressincronização completa. O Redis agora tem {} canais de referência.", hashOperations.size(REDIS_HASH_KEY));
    }

    private String formatarNomeCanal(String par, String intervalo) {
        if (par == null || par.isBlank()) {
            throw new ValidationException("O 'par' não pode ser nulo ou vazio.");
        }
        if (intervalo == null || intervalo.isBlank()) {
            throw new ValidationException("O 'intervalo' não pode ser nulo ou vazio.");
        }

        final var parSanitizado = par.trim().replace("_", "");
        final var intervaloSanitizado = traduzirIntervaloParaApi(intervalo.trim());

        return String.format("%s%s@%s", KLINE_CHANNEL_PREFIX, parSanitizado, intervaloSanitizado);
    }

    private String traduzirIntervaloParaApi(String intervaloInterno) {
        if (intervaloInterno == null || intervaloInterno.isBlank()) {
            throw new ValidationException("O intervalo não pode ser nulo ou vazio.");
        }

        return switch (intervaloInterno.toLowerCase()) {
            case "1m" -> "Min1";
            case "5m" -> "Min5";
            case "15m" -> "Min15";
            case "30m" -> "Min30";
            case "60m", "1h" -> "Min60";
            case "4h" -> "Hour4";
            case "8h" -> "Hour8";
            case "1d" -> "Day1";
            case "1w" -> "Week1";
            case "1mês", "1M" -> "Month1";
            case "min1", "min5", "min15", "min30", "min60", "hour4", "hour8", "day1", "week1", "month1" ->
                    intervaloInterno;
            default ->
                    throw new ValidationException("Intervalo '" + intervaloInterno + "' não é suportado. Use um formato como '15m', '1h', '4h', '1d'.");
        };
    }
}