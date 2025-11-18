package br.com.bot_mexc.services;

import br.com.bot_mexc.models.dtos.CandleDTO;
import br.com.bot_mexc.models.dtos.IndicadorConfigDTO;
import br.com.bot_mexc.models.dtos.ResultadoRsiEstocasticoDTO;
import br.com.bot_mexc.models.dtos.TriploRsiDTO;
import br.com.bot_mexc.models.entities.IndicadorConfig;
import br.com.bot_mexc.models.enums.TipoIndicador;
import br.com.bot_mexc.utils.CalculoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalculoIndicadorService {

    public Map<String, BigDecimal> calcularIndicadores(List<CandleDTO> candles, Set<IndicadorConfig> configs, BigDecimal ultimoClosePrice) {
        if (CollectionUtils.isEmpty(configs) || CollectionUtils.isEmpty(candles)) {
            return Collections.emptyMap();
        }

        Map<String, BigDecimal> resultados = new HashMap<>();
        List<BigDecimal> precosFechamento = candles.stream().map(CandleDTO::closeValue).collect(Collectors.toList());

        TriploRsiDTO triploRsiCache = null;
        ResultadoRsiEstocasticoDTO rsiEstocasticoCache = null;

        for (IndicadorConfig config : configs) {
            Map<String, Integer> params = config.getParametros() != null ?
                    IndicadorConfigDTO.parametrosFromJson(config.getParametros()) :
                    Collections.emptyMap();

            try {
                switch (config.getTipoIndicador()) {
                    case RSI_CURTO:
                    case RSI_MEDIO:
                    case RSI_LONGO:
                        if (triploRsiCache == null) {
                            Integer pCurto = getParam(configs, TipoIndicador.RSI_CURTO, "periodoRsiCurto", 7);
                            Integer pMedio = getParam(configs, TipoIndicador.RSI_MEDIO, "periodoRsiMedio", 14);
                            Integer pLongo = getParam(configs, TipoIndicador.RSI_LONGO, "periodoRsiLongo", 21);
                            triploRsiCache = CalculoUtils.calcularTriploRsi(candles, pCurto, pMedio, pLongo);
                        }
                        if (config.getTipoIndicador() == TipoIndicador.RSI_CURTO)
                            resultados.put(config.getAlias(), triploRsiCache.rsiCurto());
                        if (config.getTipoIndicador() == TipoIndicador.RSI_MEDIO)
                            resultados.put(config.getAlias(), triploRsiCache.rsiMedio());
                        if (config.getTipoIndicador() == TipoIndicador.RSI_LONGO)
                            resultados.put(config.getAlias(), triploRsiCache.rsiLongo());
                        break;

                    case RSI_ESTOCASTICO_K:
                    case RSI_ESTOCASTICO_D:
                        if (rsiEstocasticoCache == null) {
                            if (triploRsiCache == null) {
                                Integer pMedio = getParam(configs, TipoIndicador.RSI_MEDIO, "periodoRsiMedio", 14);
                                triploRsiCache = CalculoUtils.calcularTriploRsi(candles, 7, pMedio, 21);
                            }
                            Integer pEstocastico = params.getOrDefault("periodoRsiEstocastico", 14);
                            Integer pK = params.getOrDefault("suavizacaoRsiEstocasticoK", 3);
                            Integer pD = params.getOrDefault("suavizacaoRsiEstocasticoD", 3);
                            rsiEstocasticoCache = CalculoUtils.calcularRsiEstocasticoDeSerie(triploRsiCache.rsiMedioSerie(), pEstocastico, pK, pD);
                        }
                        if (config.getTipoIndicador() == TipoIndicador.RSI_ESTOCASTICO_K)
                            resultados.put(config.getAlias(), rsiEstocasticoCache.k());
                        if (config.getTipoIndicador() == TipoIndicador.RSI_ESTOCASTICO_D)
                            resultados.put(config.getAlias(), rsiEstocasticoCache.d());
                        break;

                    case EMA:
                        Integer pEma = params.getOrDefault("periodoEma", 200);
                        BigDecimal ema = CalculoUtils.calcularEma(precosFechamento, pEma);
                        resultados.put(config.getAlias(), ema);
                        break;

                    case SMA:
                        Integer pSma = params.getOrDefault("periodoSma", 200);
                        BigDecimal sma = CalculoUtils.calcularSma(precosFechamento, pSma);
                        resultados.put(config.getAlias(), sma);
                        break;

                    case VOLUME:
                        BigDecimal volume = candles.getLast().volume();
                        resultados.put(config.getAlias(), volume);
                        break;
                }
            } catch (Exception e) {
                log.warn("Falha ao calcular indicador {} (Alias: {}). Erro: {}", config.getTipoIndicador(), config.getAlias(), e.getMessage());
                resultados.put(config.getAlias(), BigDecimal.ZERO);
            }
        }

        resultados.put("PRECO_FECHAMENTO", ultimoClosePrice);

        return resultados;
    }

    private Integer getParam(Set<IndicadorConfig> configs, TipoIndicador tipo, String paramKey, Integer defaultVal) {
        return configs.stream()
                .filter(c -> c.getTipoIndicador() == tipo)
                .map(c -> IndicadorConfigDTO.parametrosFromJson(c.getParametros()).getOrDefault(paramKey, defaultVal))
                .findFirst()
                .orElse(defaultVal);
    }
}