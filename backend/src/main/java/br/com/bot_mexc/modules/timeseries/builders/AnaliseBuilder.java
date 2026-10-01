package br.com.bot_mexc.modules.timeseries.builders;
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

import br.com.bot_mexc.modules.timeseries.dtos.CandleDTO;
import br.com.bot_mexc.modules.strategy.dtos.IndicadorConfigDTO;
import br.com.bot_mexc.modules.timeseries.entities.Analise;
import br.com.bot_mexc.modules.strategy.entities.IndicadorConfig;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;

@UtilityClass
public class AnaliseBuilder {

    public static Analise montarAnalise(String par, String intervalo, CandleDTO candle, Collection<IndicadorConfig> configs, Map<String, BigDecimal> valoresCalculados) {
        var analise = Analise.builder()
                .par(par)
                .intervalo(intervalo)
                .valorAtualMoeda(candle.valorFechamento())
                .volume(candle.volume())
                .dataAnalise(Instant.ofEpochSecond(candle.dataAbertura()))
                .periodoRsiCurto(0)
                .periodoRsiMedio(0)
                .periodoRsiLongo(0)
                .periodoRsiEstocastico(0)
                .suavizacaoRsiEstocasticoK(0)
                .suavizacaoRsiEstocasticoD(0)
                .ema(BigDecimal.ZERO)
                .sma(BigDecimal.ZERO)
                .rsiCurto(BigDecimal.ZERO)
                .rsiMedio(BigDecimal.ZERO)
                .rsiLongo(BigDecimal.ZERO)
                .rsiEstocasticoK(BigDecimal.ZERO)
                .rsiEstocasticoD(BigDecimal.ZERO);

        for (IndicadorConfig config : configs) {
            final var params = IndicadorConfigDTO.parametrosFromJson(config.getParametros());
            final var valor = valoresCalculados.getOrDefault(config.getAlias(), BigDecimal.ZERO);

            switch (config.getTipoIndicador()) {
                case EMA -> {
                    analise.periodoEma(params.getOrDefault("periodo", 0));
                    analise.ema(valor);
                }
                case SMA -> {
                    analise.periodoSma(params.getOrDefault("periodo", 0));
                    analise.sma(valor);
                }
                case RSI_CURTO -> {
                    analise.periodoRsiCurto(params.getOrDefault("periodo", 7));
                    analise.rsiCurto(valor);
                }
                case RSI_MEDIO -> {
                    analise.periodoRsiMedio(params.getOrDefault("periodo", 14));
                    analise.rsiMedio(valor);
                }
                case RSI_LONGO -> {
                    analise.periodoRsiLongo(params.getOrDefault("periodo", 21));
                    analise.rsiLongo(valor);
                }
                case RSI_ESTOCASTICO_K -> {
                    analise.periodoRsiEstocastico(params.getOrDefault("periodoRsi", 14));
                    analise.suavizacaoRsiEstocasticoK(params.getOrDefault("suavizacaoK", 3));
                    analise.suavizacaoRsiEstocasticoD(params.getOrDefault("suavizacaoD", 3));
                    analise.rsiEstocasticoK(valor);
                }
                case RSI_ESTOCASTICO_D -> {
                    analise.periodoRsiEstocastico(params.getOrDefault("periodoRsi", 14));
                    analise.suavizacaoRsiEstocasticoK(params.getOrDefault("suavizacaoK", 3));
                    analise.suavizacaoRsiEstocasticoD(params.getOrDefault("suavizacaoD", 3));
                    analise.rsiEstocasticoD(valor);
                }
            }
        }

        return analise.build();
    }

}