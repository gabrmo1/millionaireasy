package br.com.bot_mexc.services;

import br.com.bot_mexc.models.dtos.OperacaoCacheDTO;
import br.com.bot_mexc.models.entities.Operacao;
import br.com.bot_mexc.models.enums.StatusOperacoes;
import br.com.bot_mexc.repositories.OperacaoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperacaoCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final OperacaoRepository operacaoRepository;
    private final ObjectMapper objectMapper;

    private static final String CACHE_KEY_PREFIX = "mexc:operacoes:ativas:";

    public List<OperacaoCacheDTO> getOperacoesAtivas(String par, String intervalo) {
        final var key = getCacheKey(par, intervalo);

        try {
            final var cached = redisTemplate.opsForValue().get(key);

            if (cached != null) {
                return ((List<?>) cached).stream()
                        .map(obj -> objectMapper.convertValue(obj, OperacaoCacheDTO.class))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Cache miss ou erro de desserialização para {}/{}. Recarregando do banco.", par, intervalo);
        }
        return syncOperacoesDoBanco(par, intervalo);
    }

    public void adicionarOperacao(Operacao operacao) {
        final var key = getCacheKey(operacao.getPar(), operacao.getIntervalo());
        var listaAtual = getOperacoesAtivas(operacao.getPar(), operacao.getIntervalo());

        listaAtual.removeIf(op -> op.id().equals(operacao.getId()));

        listaAtual.add(OperacaoCacheDTO.fromEntity(operacao));
        salvarNoRedis(key, listaAtual);
        log.info("Operação {} adicionada ao cache Redis.", operacao.getId());
    }

    public void removerOperacao(Operacao operacao) {
        final var key = getCacheKey(operacao.getPar(), operacao.getIntervalo());
        var listaAtual = getOperacoesAtivas(operacao.getPar(), operacao.getIntervalo());

        boolean removeu = listaAtual.removeIf(op -> op.id().equals(operacao.getId()));

        if (removeu) {
            salvarNoRedis(key, listaAtual);
            log.info("Operação {} removida do cache Redis.", operacao.getId());
        }
    }

    public void removerOperacao(String id, String par, String intervalo) {
        final var key = getCacheKey(par, intervalo);
        var listaAtual = getOperacoesAtivas(par, intervalo);

        boolean removeu = listaAtual.removeIf(op -> op.id().equals(id));

        if (removeu) {
            salvarNoRedis(key, listaAtual);
        }
    }

    private List<OperacaoCacheDTO> syncOperacoesDoBanco(String par, String intervalo) {
        log.info("Realizando Full Sync do banco para o Redis: {}/{}", par, intervalo);
        final var operacoes = operacaoRepository.findActiveOperationsEagerly(StatusOperacoes.EM_ANDAMENTO, par, intervalo);
        final var dtos = operacoes.stream().map(OperacaoCacheDTO::fromEntity).collect(Collectors.toList());

        salvarNoRedis(getCacheKey(par, intervalo), dtos);

        return dtos;
    }

    private void salvarNoRedis(String key, List<OperacaoCacheDTO> dtos) {
        redisTemplate.opsForValue().set(key, dtos, 24, TimeUnit.HOURS);
    }

    private String getCacheKey(String par, String intervalo) {
        return CACHE_KEY_PREFIX + par + ":" + intervalo;
    }
}