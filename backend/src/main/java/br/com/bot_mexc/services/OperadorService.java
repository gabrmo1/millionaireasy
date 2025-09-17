package br.com.bot_mexc.services;

import br.com.bot_mexc.models.dtos.CriarOperadorDTO;
import br.com.bot_mexc.models.dtos.OperadorDTO;
import br.com.bot_mexc.repositories.OperadorRepository;
import br.com.bot_mexc.utils.OperadorUtils;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import static br.com.bot_mexc.utils.OperadorUtils.converterDtoParaEntidade;

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