package br.com.bot_mexc.modules.timeseries.dtos;
import br.com.bot_mexc.shared.enums.*;
import br.com.bot_mexc.shared.utils.DateUtils;
import br.com.bot_mexc.shared.entities.BaseEntity;
import br.com.bot_mexc.shared.configs.RabbitMQConfig;
import br.com.bot_mexc.modules.timeseries.entities.*;
import br.com.bot_mexc.modules.timeseries.dtos.*;
import br.com.bot_mexc.modules.timeseries.repositories.*;
import br.com.bot_mexc.modules.timeseries.services.*;
import br.com.bot_mexc.modules.timeseries.utils.*;
import br.com.bot_mexc.modules.timeseries.builders.*;
import br.com.bot_mexc.modules.strategy.entities.IndicadorConfig;

import lombok.With;

import java.math.BigDecimal;

@With
public record CandleDTO(
        long dataAbertura,
        long dataFechamento,
        BigDecimal valorAbertura,
        BigDecimal valorFechamento,
        BigDecimal minima,
        BigDecimal maxima,
        BigDecimal volume
) {
}