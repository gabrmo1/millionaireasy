package br.com.bot_mexc.modules.strategy.services;
import br.com.bot_mexc.shared.enums.*;
import br.com.bot_mexc.shared.utils.DateUtils;
import br.com.bot_mexc.shared.entities.BaseEntity;
import br.com.bot_mexc.shared.configs.RabbitMQConfig;
import br.com.bot_mexc.modules.strategy.entities.*;
import br.com.bot_mexc.modules.strategy.dtos.*;
import br.com.bot_mexc.modules.strategy.repositories.*;
import br.com.bot_mexc.modules.strategy.services.*;
import br.com.bot_mexc.modules.strategy.utils.*;
import br.com.bot_mexc.modules.strategy.builders.*;
import br.com.bot_mexc.modules.strategy.services.OperacaoCacheService;
import br.com.bot_mexc.modules.strategy.services.IndicadorStateService;
import br.com.bot_mexc.modules.market.services.MexcConnectionService;
import br.com.bot_mexc.modules.market.services.mexc.MexcSubscriptionService;
import br.com.bot_mexc.modules.strategy.services.AvaliacaoCondicaoService;
import br.com.bot_mexc.modules.strategy.dtos.OperacaoCacheDTO;
import br.com.bot_mexc.modules.timeseries.repositories.AnaliseRepository;
import br.com.bot_mexc.modules.timeseries.services.BacktestCandleProviderService;
import br.com.bot_mexc.modules.timeseries.dtos.CandleDTO;
import br.com.bot_mexc.modules.timeseries.builders.AnaliseBuilder;

import br.com.bot_mexc.modules.strategy.dtos.CriarOperadorDTO;
import br.com.bot_mexc.modules.strategy.dtos.OperadorDTO;
import br.com.bot_mexc.modules.strategy.repositories.OperadorRepository;
import br.com.bot_mexc.modules.strategy.utils.OperadorUtils;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import static br.com.bot_mexc.modules.strategy.utils.OperadorUtils.converterDtoParaEntidade;

@Service
public class OperadorService {

    private final OperadorRepository operadorRepository;

    public OperadorService(OperadorRepository operadorRepository) {
        this.operadorRepository = operadorRepository;
    }

    public List<OperadorDTO> findAll() {
        final var operadores = operadorRepository.findAll();
        return operadores.stream().map(OperadorUtils::converterEntidadeParaDto).toList();
    }

    public OperadorDTO findById(String id) {
        return operadorRepository.findById(id)
                .map(OperadorUtils::converterEntidadeParaDto)
                .orElseThrow(() -> new ValidationException("Operador não encontrado."));
    }

    @Transactional
    public void criarOperador(CriarOperadorDTO request) {
        final var operadorExistente = operadorRepository.findByAccessKey(request.accessKey());

        if (operadorExistente.isPresent())
            throw new ValidationException("Já existe um operador cadastrado com esta Access Key.");

        final var operador = converterDtoParaEntidade(request);

        operadorRepository.save(operador);
    }

    @Transactional
    public void updateOperador(String id, CriarOperadorDTO request) {
        final var operador = operadorRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Operador não encontrado."));

        // Verifica se a access key foi alterada e se a nova já existe em outro operador do mesmo usuário
        if (!Objects.equals(operador.getAccessKey(), request.accessKey())) {
            operadorRepository.findByAccessKey(request.accessKey()).ifPresent(op -> {
                throw new ValidationException("Já existe um operador cadastrado com esta Access Key.");
            });
        }

        OperadorUtils.atualizarEntidadeComDto(operador, request);
        operadorRepository.save(operador);
    }

    @Transactional
    public void deleteOperador(String id) {
        if (!operadorRepository.existsById(id)) {
            throw new ValidationException("Operador não encontrado.");
        }
        operadorRepository.deleteById(id);
    }

}