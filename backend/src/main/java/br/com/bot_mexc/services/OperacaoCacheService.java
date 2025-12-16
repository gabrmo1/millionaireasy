package br.com.bot_mexc.services;

import br.com.bot_mexc.models.dtos.OperacaoCacheDTO;
import br.com.bot_mexc.models.entities.Compra;
import br.com.bot_mexc.models.entities.Operacao;
import br.com.bot_mexc.models.enums.StatusOperacoes;
import br.com.bot_mexc.repositories.CompraRepository;
import br.com.bot_mexc.repositories.OperacaoRepository;
import br.com.bot_mexc.repositories.VendaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperacaoCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final OperacaoRepository operacaoRepository;
    private final CompraRepository compraRepository;
    private final VendaRepository vendaRepository;
    private final ObjectMapper objectMapper;

    private static final String CACHE_KEY_PREFIX = "mexc:operacoes:ativas:";

    public OperacaoCacheDTO getOperacaoById(String id, String par, String intervalo) {
        List<OperacaoCacheDTO> lista = getOperacoesAtivas(par, intervalo);
        return lista.stream()
                .filter(op -> op.id().equals(id))
                .findFirst()
                .orElse(null);
    }

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
            log.warn("Cache miss/erro para {}/{}. Recarregando.", par, intervalo);
        }
        return syncOperacoesDoBanco(par, intervalo);
    }

    public void adicionarOperacao(Operacao operacao) {
        OperacaoCacheDTO dto = montarDTOComEstado(operacao);
        atualizarListaNoCache(operacao.getPar(), operacao.getIntervalo(), dto);
        log.info("Operação {} adicionada ao cache com saldo: {}", operacao.getId(), dto.saldo());
    }

    public void removerOperacao(Operacao operacao) {
        removerOperacao(operacao.getId(), operacao.getPar(), operacao.getIntervalo());
    }

    public void removerOperacao(String id, String par, String intervalo) {
        final var key = getCacheKey(par, intervalo);
        var listaAtual = getOperacoesAtivas(par, intervalo);
        boolean removeu = listaAtual.removeIf(op -> op.id().equals(id));
        if (removeu) {
            redisTemplate.opsForValue().set(key, listaAtual, 24, TimeUnit.HOURS);
        }
    }

    public void atualizarEstadoAposCompra(String idOperacao, String par, String intervalo, BigDecimal novoSaldo, BigDecimal preco, BigDecimal volume) {
        List<OperacaoCacheDTO> lista = getOperacoesAtivas(par, intervalo);

        var novaLista = lista.stream().map(op -> {
            if (op.id().equals(idOperacao)) {
                return new OperacaoCacheDTO(
                        op.id(), op.par(), op.intervalo(), op.operadorId(), op.estrategiaId(),
                        op.modoTeste(),
                        novoSaldo,
                        true,
                        preco,
                        volume,
                        op.valorOperacaoFixo(), op.percentualValorOperacao(), op.stablecoin(),
                        op.vendaApenasPorLucro(), op.percentualLucro(),
                        op.indicadores(), op.condicoesCompra(), op.condicoesVenda()
                );
            }
            return op;
        }).collect(Collectors.toList());

        redisTemplate.opsForValue().set(getCacheKey(par, intervalo), novaLista, 24, TimeUnit.HOURS);
        log.debug("Cache atualizado após COMPRA op: {}", idOperacao);
    }

    public void atualizarEstadoAposVenda(String idOperacao, String par, String intervalo, BigDecimal novoSaldo) {
        List<OperacaoCacheDTO> lista = getOperacoesAtivas(par, intervalo);

        var novaLista = lista.stream().map(op -> {
            if (op.id().equals(idOperacao)) {
                return new OperacaoCacheDTO(
                        op.id(), op.par(), op.intervalo(), op.operadorId(), op.estrategiaId(),
                        op.modoTeste(),
                        novoSaldo,
                        false,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        op.valorOperacaoFixo(), op.percentualValorOperacao(), op.stablecoin(),
                        op.vendaApenasPorLucro(), op.percentualLucro(),
                        op.indicadores(), op.condicoesCompra(), op.condicoesVenda()
                );
            }
            return op;
        }).collect(Collectors.toList());

        redisTemplate.opsForValue().set(getCacheKey(par, intervalo), novaLista, 24, TimeUnit.HOURS);
        log.debug("Cache atualizado após VENDA op: {}", idOperacao);
    }

    private void atualizarListaNoCache(String par, String intervalo, OperacaoCacheDTO dto) {
        final var key = getCacheKey(par, intervalo);
        var listaAtual = getOperacoesAtivas(par, intervalo);

        listaAtual.removeIf(op -> op.id().equals(dto.id()));
        listaAtual.add(dto);

        redisTemplate.opsForValue().set(key, listaAtual, 24, TimeUnit.HOURS);
    }

    private List<OperacaoCacheDTO> syncOperacoesDoBanco(String par, String intervalo) {
        final var operacoes = operacaoRepository.findActiveOperationsEagerly(StatusOperacoes.EM_ANDAMENTO, par, intervalo);
        final var dtos = operacoes.stream()
                .map(this::montarDTOComEstado)
                .collect(Collectors.toList());

        redisTemplate.opsForValue().set(getCacheKey(par, intervalo), dtos, 24, TimeUnit.HOURS);
        return dtos;
    }

    private OperacaoCacheDTO montarDTOComEstado(Operacao op) {
        BigDecimal saldo = Boolean.TRUE.equals(op.getModoTeste()) ? op.getSaldoInicial() : BigDecimal.ZERO;
        Optional<Compra> ultimaCompra = compraRepository.findTopByOperacaoIdOrderByDataCriacaoDesc(op.getId());

        boolean posicionado = false;
        BigDecimal preco = BigDecimal.ZERO;
        BigDecimal volume = BigDecimal.ZERO;

        if (ultimaCompra.isPresent()) {
            boolean vendaPosterior = vendaRepository.existsByOperacaoIdAndDataCriacaoAfter(op.getId(), ultimaCompra.get().getDataCriacao());

            if (!vendaPosterior) {
                posicionado = true;
                preco = ultimaCompra.get().getValor_moeda();
                volume = ultimaCompra.get().getVolume();
            }
        }

        return OperacaoCacheDTO.fromEntity(op, saldo, posicionado, preco, volume);
    }

    private String getCacheKey(String par, String intervalo) {
        return CACHE_KEY_PREFIX + par + ":" + intervalo;
    }
}