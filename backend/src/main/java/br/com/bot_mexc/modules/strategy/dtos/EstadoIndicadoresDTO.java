package br.com.bot_mexc.modules.strategy.dtos;
import br.com.bot_mexc.shared.enums.*;
import br.com.bot_mexc.shared.utils.DateUtils;
import br.com.bot_mexc.shared.constants.IndicadorKeys;
import br.com.bot_mexc.shared.configs.RabbitMQConfig;
import br.com.bot_mexc.shared.configs.RedisConfig;
import br.com.bot_mexc.modules.market.services.*;
import br.com.bot_mexc.modules.market.services.mexc.*;
import br.com.bot_mexc.modules.strategy.services.indicators.*;
import br.com.bot_mexc.modules.market.dtos.*;
import br.com.bot_mexc.modules.market.dtos.mexc.*;
import br.com.bot_mexc.modules.strategy.dtos.monitoramento.*;
import br.com.bot_mexc.modules.strategy.entities.CondicaoCompra;
import br.com.bot_mexc.modules.strategy.entities.CondicaoVenda;
import br.com.bot_mexc.modules.strategy.entities.IndicadorConfig;
import br.com.bot_mexc.modules.strategy.entities.Operacao;
import br.com.bot_mexc.modules.strategy.repositories.OperacaoRepository;
import br.com.bot_mexc.modules.strategy.repositories.CompraRepository;
import br.com.bot_mexc.modules.strategy.repositories.VendaRepository;
import br.com.bot_mexc.modules.timeseries.dtos.CandleDTO;
import br.com.bot_mexc.modules.timeseries.services.CandleService;
import br.com.bot_mexc.modules.timeseries.services.AnaliseService;
import br.com.bot_mexc.modules.timeseries.repositories.AnaliseRepository;
import br.com.bot_mexc.modules.timeseries.builders.AnaliseBuilder;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.With;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Estado atualizado para suportar retrocompatibilidade com o banco ativo (L2 Cache / Redis)
 * e estender propriedades para novos indicadores se necessário.
 */
@Builder
@With
@JsonIgnoreProperties(ignoreUnknown = true)
public record EstadoIndicadoresDTO(
        Long ultimaDataFechamento,
        BigDecimal ultimoPrecoFechamento,
        Map<String, EstadoIndicadorItem> estados
) {
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EstadoIndicadorItem(
            BigDecimal valor,
            BigDecimal avgGain, // Retrocompatibilidade (RSI legado)
            BigDecimal avgLoss, // Retrocompatibilidade (RSI legado)
            BigDecimal soma,    // Retrocompatibilidade (SMA legado)
            Map<String, Double> metadadosL2 // Suporte para extensões futuras L2
    ) {}
}