package br.com.bot_mexc.modules.strategy.services;
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


import br.com.bot_mexc.modules.timeseries.dtos.CandleDTO;
import br.com.bot_mexc.modules.strategy.dtos.IndicadorConfigDTO;
import br.com.bot_mexc.modules.strategy.dtos.PrimitiveCandle;
import br.com.bot_mexc.modules.strategy.entities.IndicadorConfig;
import br.com.bot_mexc.modules.strategy.services.indicators.IndicadorContext;
import br.com.bot_mexc.modules.strategy.services.indicators.IndicadorState;
import br.com.bot_mexc.modules.strategy.services.indicators.IndicadorStrategy;
import br.com.bot_mexc.modules.strategy.services.indicators.IndicadorStrategyRegistry;
import br.com.bot_mexc.shared.constants.IndicadorKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalculoIndicadorService {

    private final IndicadorStateService stateService;
    private final IndicadorStrategyRegistry strategyRegistry;

    /**
     * Motor otimizado (Hot-Path).
     * Branches redundantes (ifs de nulidade) removidos.
     */
    public Map<String, BigDecimal> calcularIndicadoresOtimizado(String par, String intervalo, CandleDTO candleAtual, Set<IndicadorConfig> configs) {
        final var baseState = stateService.getOrInitializeState(par, intervalo, configs, candleAtual.dataAbertura());
        if (baseState == null) return Collections.emptyMap();

        final var resultados = new HashMap<String, BigDecimal>();
        resultados.put(IndicadorKeys.RESULT_PRECO_FECHAMENTO, candleAtual.valorFechamento());

        if (baseState.ultimoPrecoFechamento() != null) {
            resultados.put(IndicadorKeys.RESULT_PREVIOUS_PREFIX + IndicadorKeys.RESULT_PRECO_FECHAMENTO, baseState.ultimoPrecoFechamento());
        }

        PrimitiveCandle pCandle = new PrimitiveCandle(
                candleAtual.dataAbertura(), candleAtual.dataFechamento(),
                candleAtual.valorAbertura().doubleValue(), candleAtual.valorFechamento().doubleValue(),
                candleAtual.minima().doubleValue(), candleAtual.maxima().doubleValue(), candleAtual.volume().doubleValue()
        );

        double previousClose = baseState.ultimoPrecoFechamento() != null ? baseState.ultimoPrecoFechamento().doubleValue() : Double.NaN;
        final var calculosRealizadosNesteTick = new HashMap<String, BigDecimal>();

        for (IndicadorConfig config : configs) {
            final var canonicalKey = stateService.generateCanonicalKey(config);
            final var alias = config.getAlias();
            final var itemState = baseState.estados().get(canonicalKey);

            if (itemState != null) {
                resultados.put(IndicadorKeys.RESULT_PREVIOUS_PREFIX + alias, itemState.valor());
            }

            if (calculosRealizadosNesteTick.containsKey(canonicalKey)) {
                resultados.put(alias, calculosRealizadosNesteTick.get(canonicalKey));
                continue;
            }

            // Confiança total na Arquitetura (Registry Fail-Fast)
            IndicadorStrategy strategy = strategyRegistry.get(config.getTipoIndicador());

            try {
                IndicadorState l1State = stateService.getL1State(par, intervalo, canonicalKey, config.getTipoIndicador());
                IndicadorContext context = new IndicadorContext(IndicadorConfigDTO.parametrosFromJson(config.getParametros()), l1State, previousClose, null);

                double valDouble = strategy.calcular(pCandle, context);
                BigDecimal valorCalculado = Double.isNaN(valDouble) ? BigDecimal.ZERO : BigDecimal.valueOf(valDouble);

                calculosRealizadosNesteTick.put(canonicalKey, valorCalculado);
                resultados.put(alias, valorCalculado);
            } catch (Exception e) {
                log.error("Erro no cálculo O(1) {}: {}", alias, e.getMessage());
            }
        }
        return resultados;
    }
}