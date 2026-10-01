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


import br.com.bot_mexc.modules.strategy.entities.IndicadorConfig;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record ContextoAnaliseDTO(
        CandleDTO candle,
        List<OperacaoCacheDTO> operacoes,
        boolean isFechamentoDeCandle
) {
    public boolean semOperacoes() {
        return operacoes == null || operacoes.isEmpty();
    }

    public String getPar() {
        return operacoes.getFirst().par();
    }

    public String getIntervalo() {
        return operacoes.getFirst().intervalo();
    }

    public Set<IndicadorConfig> extrairConfiguracoesUnicasDeIndicadores() {
        return operacoes.stream()
                .flatMap(op -> op.indicadores().stream())
                .collect(Collectors.toSet());
    }
}