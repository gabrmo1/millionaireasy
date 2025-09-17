package br.com.bot_mexc.services;

import br.com.bot_mexc.models.dtos.EstrategiaDTO;
import br.com.bot_mexc.models.entities.CondicaoCompra;
import br.com.bot_mexc.models.entities.CondicaoVenda;
import br.com.bot_mexc.models.entities.Estrategia;
import br.com.bot_mexc.repositories.EstrategiaRepository;
import br.com.bot_mexc.utils.CondicaoCompraUtils;
import br.com.bot_mexc.utils.CondicaoVendaUtils;
import br.com.bot_mexc.utils.EstrategiaUtils;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EstrategiaService {

    private final EstrategiaRepository estrategiaRepository;

    public EstrategiaService(EstrategiaRepository estrategiaRepository) {
        this.estrategiaRepository = estrategiaRepository;
    }

    public List<EstrategiaDTO> findAll() {
        return estrategiaRepository.findAllWithConditions().stream()
                .map(EstrategiaUtils::converterEntidadeParaDto)
                .collect(Collectors.toList());
    }

    public EstrategiaDTO findById(String id) {
        return estrategiaRepository.findByIdWithConditions(id)
                .map(EstrategiaUtils::converterEntidadeParaDto)
                .orElseThrow(() -> new ValidationException("Estratégia não encontrada."));
    }

    @Transactional
    public void criarEstrategia(EstrategiaDTO dto) {
        Estrategia estrategia = EstrategiaUtils.converterDtoParaEntidade(dto);

        if (!CollectionUtils.isEmpty(dto.condicoesCompra())) {
            estrategia.setCondicoesCompra(dto.condicoesCompra().stream()
                    .map(condicaoDto -> CondicaoCompraUtils.converterDtoParaEntidade(condicaoDto, estrategia))
                    .collect(Collectors.toSet()));
        }

        if (!CollectionUtils.isEmpty(dto.condicoesVenda())) {
            estrategia.setCondicoesVenda(dto.condicoesVenda().stream()
                    .map(condicaoDto -> CondicaoVendaUtils.converterDtoParaEntidade(condicaoDto, estrategia))
                    .collect(Collectors.toSet()));
        }

        estrategiaRepository.save(estrategia);
    }

    @Transactional
    public void updateEstrategia(String id, EstrategiaDTO dto) {
        final var estrategia = estrategiaRepository.findByIdWithConditions(id)
                .orElseThrow(() -> new ValidationException("Estratégia não encontrada."));

        EstrategiaUtils.atualizarEntidadeComDto(estrategia, dto);

        // Gerencia Condições de Compra
        if (estrategia.getCondicoesCompra() == null) {
            estrategia.setCondicoesCompra(new HashSet<>());
        }
        estrategia.getCondicoesCompra().clear();
        if (!CollectionUtils.isEmpty(dto.condicoesCompra())) {
            estrategia.getCondicoesCompra().addAll(dto.condicoesCompra().stream()
                    .map(condicaoDto -> CondicaoCompraUtils.converterDtoParaEntidade(condicaoDto, estrategia))
                    .collect(Collectors.toSet()));
        }

        // Gerencia Condições de Venda
        if (estrategia.getCondicoesVenda() == null) {
            estrategia.setCondicoesVenda(new HashSet<>());
        }
        estrategia.getCondicoesVenda().clear();
        if (!CollectionUtils.isEmpty(dto.condicoesVenda())) {
            estrategia.getCondicoesVenda().addAll(dto.condicoesVenda().stream()
                    .map(condicaoDto -> CondicaoVendaUtils.converterDtoParaEntidade(condicaoDto, estrategia))
                    .collect(Collectors.toSet()));
        }

        estrategiaRepository.save(estrategia);
    }

    @Transactional
    public void deleteEstrategia(String id) {
        if (!estrategiaRepository.existsById(id)) {
            throw new ValidationException("Estratégia não encontrada.");
        }
        estrategiaRepository.deleteById(id);
    }
}