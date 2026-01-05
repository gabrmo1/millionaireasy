package br.com.bot_mexc.services;

import br.com.bot_mexc.models.dtos.CandleDTO;
import br.com.bot_mexc.models.dtos.IndicadorConfigDTO;
import br.com.bot_mexc.models.entities.IndicadorConfig;
import br.com.bot_mexc.models.enums.TipoIndicador;
import br.com.bot_mexc.utils.CalculoUtils;
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

    public Map<String, BigDecimal> calcularIndicadoresOtimizado(String par, String intervalo, CandleDTO candleAtual, Set<IndicadorConfig> configs) {
        final var baseState = stateService.getOrInitializeState(par, intervalo, configs, candleAtual.dataAbertura());
        if (baseState == null) return Collections.emptyMap();

        final var resultados = new HashMap<String, BigDecimal>();
        resultados.put("PRECO_FECHAMENTO", candleAtual.valorFechamento());

        if (baseState.ultimoPrecoFechamento() != null)
            resultados.put("PREVIOUS_PRECO_FECHAMENTO", baseState.ultimoPrecoFechamento());

        final var calculosRealizadosNesteTick = new HashMap<String, BigDecimal>();

        for (IndicadorConfig config : configs) {
            final var canonicalKey = stateService.generateCanonicalKey(config);
            final var alias = config.getAlias();

            final var itemState = baseState.estados().get(canonicalKey);
            if (itemState != null)
                resultados.put("PREVIOUS_" + alias, itemState.valor());

            if (calculosRealizadosNesteTick.containsKey(canonicalKey)) {
                resultados.put(alias, calculosRealizadosNesteTick.get(canonicalKey));
                continue;
            }

            final var params = IndicadorConfigDTO.parametrosFromJson(config.getParametros());

            if (itemState == null) {
                resultados.put(alias, BigDecimal.ZERO);
                continue;
            }

            try {
                final var valorCalculado = switch (config.getTipoIndicador()) {
                    case EMA -> {
                        int pEma = params.getOrDefault("periodoEma", 200);
                        yield CalculoUtils.calcularEmaIncremental(candleAtual.valorFechamento(), itemState.valor(), pEma);
                    }
                    case RSI_CURTO, RSI_MEDIO, RSI_LONGO -> {
                        int pRsi = getPeriodoRsi(config, params);
                        yield CalculoUtils.calcularRsiIncremental(
                                candleAtual.valorFechamento(),
                                baseState.ultimoPrecoFechamento(),
                                itemState.avgGain(),
                                itemState.avgLoss(),
                                pRsi
                        );
                    }
                    case VOLUME -> candleAtual.volume();
                    default -> itemState.valor();
                };

                calculosRealizadosNesteTick.put(canonicalKey, valorCalculado);
                resultados.put(alias, valorCalculado);

            } catch (Exception e) {
                log.error("Erro calc incremental {}: {}", alias, e.getMessage());
            }
        }
        return resultados;
    }

    private int getPeriodoRsi(IndicadorConfig c, Map<String, Integer> p) {
        if (c.getTipoIndicador() == TipoIndicador.RSI_CURTO) return p.getOrDefault("periodoRsiCurto", 7);
        if (c.getTipoIndicador() == TipoIndicador.RSI_MEDIO) return p.getOrDefault("periodoRsiMedio", 14);

        return p.getOrDefault("periodoRsiLongo", 21);
    }
}