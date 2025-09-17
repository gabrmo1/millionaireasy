package br.com.bot_mexc.builders;

import br.com.bot_mexc.models.dtos.ResultadoRsiEstocasticoDTO;
import br.com.bot_mexc.models.dtos.TriploRsiDTO;
import br.com.bot_mexc.models.entities.Analise;
import br.com.bot_mexc.utils.DateUtils;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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