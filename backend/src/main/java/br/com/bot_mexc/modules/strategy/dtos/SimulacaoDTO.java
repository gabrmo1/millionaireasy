package br.com.bot_mexc.modules.strategy.dtos;
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

import br.com.bot_mexc.shared.enums.StatusOperacoes;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record SimulacaoDTO(
        String id,
        String par,
        String intervalo,
        StatusOperacoes status,
        EstrategiaDTO estrategia,
        BigDecimal saldoInicial,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant dataCriacao,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant dataInicio,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant dataFim
) {}