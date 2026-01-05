package br.com.bot_mexc.builders;

import br.com.bot_mexc.models.dtos.CandleDTO;
import br.com.bot_mexc.models.dtos.IndicadorConfigDTO;
import br.com.bot_mexc.models.dtos.ResultadoRsiEstocasticoDTO;
import br.com.bot_mexc.models.dtos.TriploRsiDTO;
import br.com.bot_mexc.models.entities.Analise;
import br.com.bot_mexc.models.entities.IndicadorConfig;
import br.com.bot_mexc.utils.DateUtils;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;

@UtilityClass
public class AnaliseBuilder {

    public static Analise montarAnalise(String par, String intervalo, BigDecimal valorAtualMoeda, Integer periodoEma, Integer periodoSma,
                                        Integer periodoRsiCurto, Integer periodoRsiMedio, Integer periodoRsiLongo,
                                        Integer periodoRsiEstocastico, Integer suavizacaoRsiEstocasticoD,
                                        Integer suaviazacaoRsiEstocasticoK, BigDecimal ema, BigDecimal sma,
                                        BigDecimal volume, TriploRsiDTO triploRsi, ResultadoRsiEstocasticoDTO rsiEstocastico) {
        final var horarioAtual = DateUtils.agora();
        return montarAnalise(par, intervalo, valorAtualMoeda, horarioAtual, periodoEma, periodoSma, periodoRsiCurto, periodoRsiMedio,
                periodoRsiLongo, periodoRsiEstocastico, suavizacaoRsiEstocasticoD, suaviazacaoRsiEstocasticoK, ema, sma, triploRsi.rsiCurto(),
                triploRsi.rsiMedio(), triploRsi.rsiLongo(), rsiEstocastico.d(), rsiEstocastico.k(), volume);
    }

    public static Analise montarAnalise(String par, String intervalo, CandleDTO candle, Collection<IndicadorConfig> configs, Map<String, BigDecimal> valoresCalculados) {
        var analise = Analise.builder()
                .par(par)
                .intervalo(intervalo)
                .valorAtualMoeda(candle.valorFechamento())
                .volume(candle.volume())
                .dataAnalise(DateUtils.agora())
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

    private static Analise montarAnalise(String par, String intervalo, BigDecimal valorAtualMoeda, LocalDateTime dataAnalise,
                                         Integer periodoEma, Integer periodoSma, Integer periodoRsiCurto, Integer periodoRsiMedio,
                                         Integer periodoRsiLongo, Integer periodoRsiEstocastico, Integer suavizacaoRsiEstocasticoD,
                                         Integer suaviazacaoRsiEstocasticoK, BigDecimal ema, BigDecimal sma, BigDecimal rsiCurto,
                                         BigDecimal rsiMedio, BigDecimal rsiLongo, BigDecimal rsiEstocasticoD,
                                         BigDecimal rsiEstocasticoK, BigDecimal volume) {
        return Analise.builder()
                .par(par)
                .intervalo(intervalo)
                .valorAtualMoeda(valorAtualMoeda)
                .dataAnalise(dataAnalise)
                .periodoEma(periodoEma)
                .periodoSma(periodoSma)
                .periodoRsiCurto(periodoRsiCurto)
                .periodoRsiMedio(periodoRsiMedio)
                .periodoRsiLongo(periodoRsiLongo)
                .periodoRsiEstocastico(periodoRsiEstocastico)
                .suavizacaoRsiEstocasticoD(suavizacaoRsiEstocasticoD)
                .suavizacaoRsiEstocasticoK(suaviazacaoRsiEstocasticoK)
                .ema(ema)
                .sma(sma)
                .rsiCurto(rsiCurto)
                .rsiMedio(rsiMedio)
                .rsiLongo(rsiLongo)
                .rsiEstocasticoD(rsiEstocasticoD)
                .rsiEstocasticoK(rsiEstocasticoK)
                .volume(volume)
                .build();
    }

}