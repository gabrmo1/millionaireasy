package br.com.bot_mexc.models.dtos;

import br.com.bot_mexc.models.enums.TipoIndicador;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;

import java.util.Collections;
import java.util.Map;

@Builder
public record IndicadorConfigDTO(
        String id,
        String alias,
        TipoIndicador tipoIndicador,
        Map<String, Integer> parametros
) {
    public static Map<String, Integer> parametrosFromJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }
}