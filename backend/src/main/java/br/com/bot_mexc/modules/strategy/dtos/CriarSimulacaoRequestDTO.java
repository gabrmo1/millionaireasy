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

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record CriarSimulacaoRequestDTO(
        @NotBlank @Size(max = 20) String par,
        @NotBlank @Size(max = 5) String intervalo,
        @NotBlank String idEstrategia,
        @NotNull @Positive BigDecimal saldoInicial,

        @NotNull
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant dataInicio,

        @NotNull
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant dataFim
) {
    public CriarSimulacaoRequestDTO {
        if (dataInicio != null && dataFim != null && dataInicio.isAfter(dataFim)) {
            throw new IllegalArgumentException("A dataInicio não pode ser posterior a dataFim");
        }
    }
}