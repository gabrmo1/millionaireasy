package br.com.bot_mexc.services;

import br.com.bot_mexc.builders.OperacaoBuilder;
import br.com.bot_mexc.models.dtos.CriarOperacaoDTO;
import br.com.bot_mexc.models.dtos.OperacaoDTO;
import br.com.bot_mexc.models.entities.Estrategia;
import br.com.bot_mexc.models.entities.Operador;
import br.com.bot_mexc.repositories.EstrategiaRepository;
import br.com.bot_mexc.repositories.OperacaoRepository;
import br.com.bot_mexc.repositories.OperadorRepository;
import br.com.bot_mexc.utils.OperacoesUtils;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class OperacoesService {

    private final OperadorRepository operadorRepository;
    private final OperacaoRepository operacaoRepository;
    private final EstrategiaRepository estrategiaRepository;
    private final MexcService mexcService;

    public OperacoesService(OperacaoRepository operacaoRepository, OperadorRepository operadorRepository,
                            EstrategiaRepository estrategiaRepository, MexcService mexcService) {
        this.operacaoRepository = operacaoRepository;
        this.operadorRepository = operadorRepository;
        this.estrategiaRepository = estrategiaRepository;
        this.mexcService = mexcService;
    }

    public List<OperacaoDTO> findAll() {
        final var operacoes = operacaoRepository.findAllEagerly();
        return operacoes.stream().map(OperacoesUtils::converterEntidadeParaDto).toList();
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

        operacaoRepository.save(novaOperacao);
    }

    @Transactional
    public void updateOperacao(String id, CriarOperacaoDTO request) {
        final var entidades = validarEObterEntidades(request);
        var operacao = operacaoRepository.findById(id).orElseThrow(() -> new ValidationException("Operação não encontrada."));

        OperacoesUtils.atualizarEntidadeComDto(operacao, request, entidades.operador(), entidades.estrategia());
        operacaoRepository.save(operacao);
    }

    @Transactional
    public void deletarOperacao(String id) {
        if (!operacaoRepository.existsById(id))
            throw new ValidationException("Operação não encontrada.");

        operacaoRepository.deleteById(id);
    }

    private EntidadesOperacao validarEObterEntidades(CriarOperacaoDTO request) {
        final var operador = operadorRepository.findById(request.idOperador()).orElseThrow(() -> new ValidationException("Operador não encontrado."));
        Estrategia estrategia = null;

        if (request.idEstrategia() != null && !request.idEstrategia().isBlank())
            estrategia = estrategiaRepository.findById(request.idEstrategia()).orElseThrow(() -> new ValidationException("Estratégia com ID " + request.idEstrategia() + " não encontrada."));

        if (estrategia != null)
            validarCompatibilidadeEstrategiaPar(request.par(), estrategia);

        return new EntidadesOperacao(operador, estrategia);
    }

    private void validarCompatibilidadeEstrategiaPar(String par, Estrategia estrategia) {
        if (Objects.isNull(estrategia.getValorOperacaoFixo()))
            return;

        Set<String> stablecoins = mexcService.getStablecoins();
        String quoteAsset = "";

        for (String stable : stablecoins) {
            if (par.endsWith(stable)) {
                quoteAsset = stable;
                break;
            }
        }

        if (quoteAsset.isEmpty())
            throw new ValidationException("O par selecionado (" + par + ") não é um par de stablecoin válido.");

        if (!Objects.equals(estrategia.getStablecoin(), quoteAsset))
            throw new ValidationException("A estratégia selecionada opera com " + estrategia.getStablecoin() + ", mas o par selecionado (" + par + ") opera com " + quoteAsset + ".");
    }

    private record EntidadesOperacao(Operador operador, Estrategia estrategia) {
    }
}