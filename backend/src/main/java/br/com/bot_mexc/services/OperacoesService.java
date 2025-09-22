package br.com.bot_mexc.services;

import br.com.bot_mexc.builders.OperacaoBuilder;
import br.com.bot_mexc.models.dtos.CriarOperacaoDTO;
import br.com.bot_mexc.models.dtos.OperacaoDTO;
import br.com.bot_mexc.models.entities.Estrategia;
import br.com.bot_mexc.models.enums.TipoMoedaValorOperacao;
import br.com.bot_mexc.repositories.EstrategiaRepository;
import br.com.bot_mexc.repositories.OperacaoRepository;
import br.com.bot_mexc.repositories.OperadorRepository;
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

    private final OperadorRepository operadorRepository;
    private final OperacaoRepository operacaoRepository;
    private final EstrategiaRepository estrategiaRepository;

    public OperacoesService(OperacaoRepository operacaoRepository, OperadorRepository operadorRepository,
                            EstrategiaRepository estrategiaRepository) {
        this.operacaoRepository = operacaoRepository;
        this.operadorRepository = operadorRepository;
        this.estrategiaRepository = estrategiaRepository;
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
        final var operador = operadorRepository.findById(request.idOperador())
                .orElseThrow(() -> new ValidationException("Operador não encontrado."));

        final var estrategia = estrategiaRepository.findById(request.idEstrategia())
                .orElseThrow(() -> new ValidationException("Estratégia não encontrada."));

        validarCompatibilidadeEstrategiaPar(estrategia);

        operacaoRepository.save(OperacaoBuilder.montarOperacao(request, operador, estrategia));
    }

    @Transactional
    public void updateOperacao(String id, CriarOperacaoDTO request) {
        final var operacao = operacaoRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Operação não encontrada."));

        final var operador = operadorRepository.findById(request.idOperador())
                .orElseThrow(() -> new ValidationException("Operador não encontrado."));

        final var estrategia = estrategiaRepository.findById(request.idEstrategia())
                .orElseThrow(() -> new ValidationException("Estratégia não encontrada."));

        validarCompatibilidadeEstrategiaPar(estrategia);

        operacao.setPar(request.par());
        operacao.setIntervalo(request.intervalo());
        operacao.setOperador(operador);
        operacao.setEstrategia(estrategia);

        operacaoRepository.save(operacao);
    }

    @Transactional
    public void deletarOperacao(String id) {
        if (!operacaoRepository.existsById(id)) {
            throw new ValidationException("Operação não encontrada.");
        }
        operacaoRepository.deleteById(id);
    }

    private void validarCompatibilidadeEstrategiaPar(Estrategia estrategia) {
        if (Objects.nonNull(estrategia.getValorOperacaoFixo()) &&
                estrategia.getTipoMoedaValorOperacao() == TipoMoedaValorOperacao.BASE) {
            throw new ValidationException(
                    "Incompatibilidade: A estratégia está configurada para usar a MOEDA BASE em operações de valor fixo. Para pares com Stablecoins, a estratégia deve ser configurada para usar a MOEDA DE COTAÇÃO (QUOTE)."
            );
        }
    }
}