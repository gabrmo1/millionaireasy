package br.com.bot_mexc.utils;

import br.com.bot_mexc.models.dtos.IndicadorConfigDTO;
import br.com.bot_mexc.models.entities.Estrategia;
import br.com.bot_mexc.models.entities.IndicadorConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;

import java.util.Map;

@UtilityClass
public class IndicadorConfigUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static IndicadorConfig converterDtoParaEntidade(IndicadorConfigDTO dto, Estrategia estrategia) {
        IndicadorConfig entidade = new IndicadorConfig();
        entidade.setEstrategia(estrategia);
        entidade.setAlias(dto.alias());
        entidade.setTipoIndicador(dto.tipoIndicador());
        entidade.setParametros(parametrosParaJson(dto.parametros()));
        return entidade;
    }

    public static IndicadorConfigDTO converterEntidadeParaDto(IndicadorConfig entidade) {
        return IndicadorConfigDTO.builder()
                .id(entidade.getId())
                .alias(entidade.getAlias())
                .tipoIndicador(entidade.getTipoIndicador())
                .parametros(IndicadorConfigDTO.parametrosFromJson(entidade.getParametros()))
                .build();
    }

    private static String parametrosParaJson(Map<String, Integer> parametrosMap) {
        try {
            return objectMapper.writeValueAsString(parametrosMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao serializar parâmetros do indicador para JSON", e);
        }
    }
}