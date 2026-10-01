package br.com.bot_mexc.modules.strategy.utils;
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

import br.com.bot_mexc.modules.strategy.dtos.IndicadorConfigDTO;
import br.com.bot_mexc.modules.strategy.entities.Estrategia;
import br.com.bot_mexc.modules.strategy.entities.IndicadorConfig;
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