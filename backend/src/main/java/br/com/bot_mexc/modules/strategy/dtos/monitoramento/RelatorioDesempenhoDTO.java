package br.com.bot_mexc.modules.strategy.dtos.monitoramento;
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


import java.math.BigDecimal;
import java.time.Instant;

public record RelatorioDesempenhoDTO(
        Instant periodoInicio,
        Instant periodoFim,
        Integer totalTrades,
        Integer tradesVencedores,
        Integer tradesPerdedores,
        Integer operacoesZeradas,
        BigDecimal lucroBruto,
        BigDecimal prejuizoBruto,
        BigDecimal lucroLiquido,
        BigDecimal fatorLucro,
        BigDecimal maxDrawdownPercentual,
        BigDecimal maxDrawdownNominal,
        BigDecimal maeMedioPercentual,
        BigDecimal maiorVitoria,
        BigDecimal maiorDerrota,
        String tempoMedioOperacao, // Representado em ISO-8601 (ex: PT1H30M) ou formatado
        Integer maiorSequenciaVencedora,
        Integer maiorSequenciaPerdedora
) {}