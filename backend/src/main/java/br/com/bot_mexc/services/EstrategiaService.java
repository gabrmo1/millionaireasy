package br.com.bot_mexc.services;

import br.com.bot_mexc.models.dtos.CriarEstrategiaDTO;
import br.com.bot_mexc.models.dtos.EstrategiaDTO;
import br.com.bot_mexc.models.dtos.IndicadorConfigDTO;
import br.com.bot_mexc.models.entities.Estrategia;
import br.com.bot_mexc.models.enums.TipoOperando;
import br.com.bot_mexc.repositories.EstrategiaRepository;
import br.com.bot_mexc.utils.CondicaoCompraUtils;
import br.com.bot_mexc.utils.CondicaoVendaUtils;
import br.com.bot_mexc.utils.EstrategiaUtils;
import br.com.bot_mexc.utils.IndicadorConfigUtils;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    public void criarEstrategia(CriarEstrategiaDTO dto) {
        validateBusinessRules(dto);
        Estrategia estrategia = EstrategiaUtils.converterDtoParaEntidade(dto);

        estrategia.setIndicadoresConfig(dto.indicadoresConfig().stream()
                .map(indicadorDto -> IndicadorConfigUtils.converterDtoParaEntidade(indicadorDto, estrategia))
                .collect(Collectors.toSet()));

        if (!CollectionUtils.isEmpty(dto.condicoesCompra())) {
            estrategia.setCondicoesCompra(dto.condicoesCompra().stream()
                    .map(condicaoDto -> CondicaoCompraUtils.converterDtoParaEntidade(condicaoDto, estrategia))
                    .collect(Collectors.toList()));
        }

        if (!CollectionUtils.isEmpty(dto.condicoesVenda())) {
            estrategia.setCondicoesVenda(dto.condicoesVenda().stream()
                    .map(condicaoDto -> CondicaoVendaUtils.converterDtoParaEntidade(condicaoDto, estrategia))
                    .collect(Collectors.toList()));
        }

        estrategiaRepository.save(estrategia);
    }

    @Transactional
    public void updateEstrategia(String id, CriarEstrategiaDTO dto) {
        final var estrategia = estrategiaRepository.findByIdWithConditions(id)
                .orElseThrow(() -> new ValidationException("Estratégia não encontrada."));
        validateBusinessRules(dto);
        EstrategiaUtils.atualizarEntidadeComDto(estrategia, dto);

        if (estrategia.getIndicadoresConfig() == null) estrategia.setIndicadoresConfig(new HashSet<>());
        estrategia.getIndicadoresConfig().clear();
        estrategia.getIndicadoresConfig().addAll(dto.indicadoresConfig().stream()
                .map(indicadorDto -> IndicadorConfigUtils.converterDtoParaEntidade(indicadorDto, estrategia))
                .toList());

        if (estrategia.getCondicoesCompra() == null) estrategia.setCondicoesCompra(new ArrayList<>());
        estrategia.getCondicoesCompra().clear();
        if (!CollectionUtils.isEmpty(dto.condicoesCompra())) {
            estrategia.getCondicoesCompra().addAll(dto.condicoesCompra().stream()
                    .map(condicaoDto -> CondicaoCompraUtils.converterDtoParaEntidade(condicaoDto, estrategia))
                    .toList());
        }

        if (estrategia.getCondicoesVenda() == null) estrategia.setCondicoesVenda(new ArrayList<>());
        estrategia.getCondicoesVenda().clear();
        if (!CollectionUtils.isEmpty(dto.condicoesVenda())) {
            estrategia.getCondicoesVenda().addAll(dto.condicoesVenda().stream()
                    .map(condicaoDto -> CondicaoVendaUtils.converterDtoParaEntidade(condicaoDto, estrategia))
                    .toList());
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

    private void validateBusinessRules(CriarEstrategiaDTO dto) {
        Set<String> aliases = dto.indicadoresConfig().stream()
                .map(IndicadorConfigDTO::alias)
                .collect(Collectors.toSet());
        if (aliases.size() < dto.indicadoresConfig().size()) {
            throw new ValidationException("O nome (alias) de cada indicador deve ser único dentro da estratégia.");
        }

        if (!CollectionUtils.isEmpty(dto.condicoesCompra())) {
            dto.condicoesCompra().forEach(cond -> {
                if (cond.operandoATipo() == TipoOperando.INDICADOR && !aliases.contains(cond.operandoAReferencia()))
                    throw new ValidationException("Condição de compra inválida. O indicador '" + cond.operandoAReferencia() + "' não foi configurado.");
                if (cond.operandoBTipo() == TipoOperando.INDICADOR && !aliases.contains(cond.operandoBReferencia()))
                    throw new ValidationException("Condição de compra inválida. O indicador '" + cond.operandoBReferencia() + "' não foi configurado.");
            });
        }

        if (!CollectionUtils.isEmpty(dto.condicoesVenda())) {
            dto.condicoesVenda().forEach(cond -> {
                if (cond.operandoATipo() == TipoOperando.INDICADOR && !aliases.contains(cond.operandoAReferencia()))
                    throw new ValidationException("Condição de venda inválida. O indicador '" + cond.operandoAReferencia() + "' não foi configurado.");
                if (cond.operandoBTipo() == TipoOperando.INDICADOR && !aliases.contains(cond.operandoBReferencia()))
                    throw new ValidationException("Condição de venda inválida. O indicador '" + cond.operandoBReferencia() + "' não foi configurado.");
            });
        }
    }
}