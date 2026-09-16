package br.com.bot_mexc.services;

import br.com.bot_mexc.models.dtos.CandleDTO;
import br.com.bot_mexc.models.dtos.IndicadorConfigDTO;
import br.com.bot_mexc.models.dtos.PrimitiveCandle;
import br.com.bot_mexc.models.entities.IndicadorConfig;
import br.com.bot_mexc.services.strategy.IndicadorContext;
import br.com.bot_mexc.services.strategy.IndicadorState;
import br.com.bot_mexc.services.strategy.IndicadorStrategy;
import br.com.bot_mexc.services.strategy.IndicadorStrategyRegistry;
import br.com.bot_mexc.utils.constants.IndicadorKeys;
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