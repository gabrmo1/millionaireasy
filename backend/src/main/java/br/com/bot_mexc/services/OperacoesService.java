package br.com.bot_mexc.services;

import br.com.bot_mexc.builders.OperacaoBuilder;
import br.com.bot_mexc.models.dtos.CriarOperacaoDTO;
import br.com.bot_mexc.models.dtos.EntidadesOperacaoDTO;
import br.com.bot_mexc.models.dtos.OperacaoDTO;
import br.com.bot_mexc.models.entities.Estrategia;
import br.com.bot_mexc.models.entities.Operacao;
import br.com.bot_mexc.models.enums.StatusOperacoes;
import br.com.bot_mexc.repositories.EstrategiaRepository;
import br.com.bot_mexc.repositories.OperacaoRepository;
import br.com.bot_mexc.repositories.OperadorRepository;
import br.com.bot_mexc.utils.DateUtils;
import br.com.bot_mexc.utils.OperacoesUtils;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class OperacoesService {

    private final MexcSubscriptionService subscriptionService;
    private final OperacaoCacheService operacaoCacheService;
    private final EstrategiaRepository estrategiaRepository;
    private final OperadorRepository operadorRepository;
    private final OperacaoRepository operacaoRepository;
    private final MexcService mexcService;

    public OperacoesService(OperacaoRepository operacaoRepository, OperadorRepository operadorRepository,
                            EstrategiaRepository estrategiaRepository, MexcService mexcService,
                            MexcSubscriptionService subscriptionService, OperacaoCacheService operacaoCacheService) {
        this.estrategiaRepository = estrategiaRepository;
        this.operacaoCacheService = operacaoCacheService;
        this.subscriptionService = subscriptionService;
        this.operacaoRepository = operacaoRepository;
        this.operadorRepository = operadorRepository;
        this.mexcService = mexcService;
    }

    public List<OperacaoDTO> findAll() {
        return operacaoRepository.findAllEagerly().stream()
                .map(OperacoesUtils::converterEntidadeParaDto)
                .toList();
    }

    public OperacaoDTO findById(String id) {
        return operacaoRepository.findById(id)
                .map(OperacoesUtils::converterEntidadeParaDto)
                .orElseThrow(() -> new ValidationException("Operação não encontrada."));
    }

    @Transactional
    public void criarOperacao(CriarOperacaoDTO request) {
        final var entidades = validarEObterEntidades(request);
        final var novaOperacao = OperacaoBuilder.montarOperacao(request, entidades.operador(), entidades.estrategia());

        novaOperacao.setStatus(StatusOperacoes.PARADO);

        operacaoRepository.save(novaOperacao);
        log.info("Operação {} criada com status PARADO.", novaOperacao.getId());
    }

    @Transactional
    public void iniciarOperacao(String id) {
        var operacao = operacaoRepository.findById(id).orElseThrow(() -> new ValidationException("Operação não encontrada."));
        validarInicioOperacao(operacao);

        operacao.setStatus(StatusOperacoes.EM_ANDAMENTO);
        operacao.setDataInicio(DateUtils.agora());
        operacao.setDataFim(null);
        operacaoRepository.save(operacao);

        operacaoCacheService.adicionarOperacao(operacao);
        subscriptionService.addSubscription(operacao.getPar(), operacao.getIntervalo());
        log.info("Operação {} iniciada.", id);
    }

    @Transactional
    public void pararOperacao(String id) {
        var operacao = operacaoRepository.findById(id).orElseThrow(() -> new ValidationException("Operação não encontrada."));
        if (operacao.getStatus() == StatusOperacoes.PARADO)
            throw new ValidationException("A operação já está parada.");

        operacao.setStatus(StatusOperacoes.PARADO);
        operacao.setDataFim(DateUtils.agora());
        operacaoRepository.save(operacao);

        operacaoCacheService.removerOperacao(operacao);
        subscriptionService.removeSubscription(operacao.getPar(), operacao.getIntervalo());
        log.info("Operação {} parada.", id);
    }

    @Transactional
    public void updateOperacao(String id, CriarOperacaoDTO request) {
        var operacao = operacaoRepository.findById(id).orElseThrow(() -> new ValidationException("Operação não encontrada."));
        final var estavaEmAndamento = operacao.getStatus() == StatusOperacoes.EM_ANDAMENTO;
        final var parAntigo = operacao.getPar();
        final var intervaloAntigo = operacao.getIntervalo();
        final var entidades = validarEObterEntidades(request);

        OperacoesUtils.atualizarEntidadeComDto(operacao, request, entidades.operador(), entidades.estrategia());

        operacao.setStatus(StatusOperacoes.PARADO);
        operacao.setDataFim(DateUtils.agora());

        operacaoRepository.save(operacao);

        if (estavaEmAndamento) {
            operacaoCacheService.removerOperacao(id, parAntigo, intervaloAntigo);
            subscriptionService.removeSubscription(parAntigo, intervaloAntigo);
            log.info("Operação {} atualizada e forçada para PARADO. Cache e WS antigos removidos.", id);
        }
    }

    @Transactional
    public void deletarOperacao(String id) {
        var operacao = operacaoRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Operação não encontrada."));

        if (operacao.getStatus() == StatusOperacoes.EM_ANDAMENTO) {
            operacaoCacheService.removerOperacao(operacao);
            subscriptionService.removeSubscription(operacao.getPar(), operacao.getIntervalo());
        }

        operacaoRepository.deleteById(id);
        log.info("Operação {} deletada.", id);
    }

    private void validarInicioOperacao(Operacao operacao) {
        if (operacao.getStatus() == StatusOperacoes.EM_ANDAMENTO)
            throw new ValidationException("A operação já está em andamento.");
        if (operacao.getEstrategia() == null)
            throw new ValidationException("Estratégia não definida.");
        if (operacao.getOperador() == null)
            throw new ValidationException("Operador não definido.");
    }

    private EntidadesOperacaoDTO validarEObterEntidades(CriarOperacaoDTO request) {
        final var operador = operadorRepository.findById(request.idOperador())
                .orElseThrow(() -> new ValidationException("Operador não encontrado."));

        Estrategia estrategia = null;
        if (request.idEstrategia() != null && !request.idEstrategia().isBlank())
            estrategia = estrategiaRepository.findById(request.idEstrategia()).orElseThrow(() -> new ValidationException("Estratégia com ID " + request.idEstrategia() + " não encontrada."));
        if (estrategia != null)
            validarCompatibilidadeEstrategiaPar(request.par(), estrategia);

        return new EntidadesOperacaoDTO(operador, estrategia);
    }

    private void validarCompatibilidadeEstrategiaPar(String par, Estrategia estrategia) {
        if (Objects.isNull(estrategia.getValorOperacaoFixo()))
            return;

        final var stablecoins = mexcService.getStablecoins();
        var quoteAsset = "";
        for (String stable : stablecoins) {
            if (par.endsWith(stable)) {
                quoteAsset = stable;
                break;
            }
        }
        if (quoteAsset.isEmpty())
            throw new ValidationException("Par inválido.");
        if (!Objects.equals(estrategia.getStablecoin(), quoteAsset))
            throw new ValidationException("Incompatibilidade de stablecoin.");
    }
}