package br.com.bot_mexc.utils;

import br.com.bot_mexc.models.dtos.CandleDTO;
import br.com.bot_mexc.models.dtos.ResultadoRsiEstocasticoDTO;
import br.com.bot_mexc.models.dtos.TriploRsiDTO;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@UtilityClass
public class CalculoUtils {

    public static BigDecimal calcularEma(List<BigDecimal> precos, int periodo) {
        if (precos.size() < periodo)
            return BigDecimal.ZERO;

        final var multiplicador = BigDecimal.valueOf(2).divide(BigDecimal.valueOf(periodo + 1), 8, RoundingMode.HALF_UP);
        final var precosRecentes = precos.subList(precos.size() - periodo, precos.size());
        var somaInicial = precosRecentes.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        var ema = somaInicial.divide(BigDecimal.valueOf(periodo), 8, RoundingMode.HALF_UP);

        for (BigDecimal precoFechamento : precosRecentes) {
            ema = precoFechamento.multiply(multiplicador).add(ema.multiply(BigDecimal.ONE.subtract(multiplicador)));
        }

        return ema;
    }

    public static BigDecimal calcularEmaIncremental(BigDecimal precoAtual, BigDecimal emaAnterior, int periodo) {
        if (emaAnterior == null)
            return precoAtual;

        final var multiplicador = BigDecimal.valueOf(2).divide(BigDecimal.valueOf(periodo + 1), 8, RoundingMode.HALF_UP);

        return precoAtual.subtract(emaAnterior)
                .multiply(multiplicador)
                .add(emaAnterior)
                .setScale(8, RoundingMode.HALF_UP);
    }

    public static BigDecimal calcularRsiIncremental(BigDecimal precoAtual, BigDecimal precoAnterior,
                                                    BigDecimal mediaGanhoAnt, BigDecimal mediaPerdaAnt, int periodo) {
        var variacao = precoAtual.subtract(precoAnterior);
        var ganho = variacao.compareTo(BigDecimal.ZERO) > 0 ? variacao : BigDecimal.ZERO;
        var perda = variacao.compareTo(BigDecimal.ZERO) < 0 ? variacao.abs() : BigDecimal.ZERO;

        var novaMediaGanho = mediaGanhoAnt.multiply(BigDecimal.valueOf(periodo - 1))
                .add(ganho)
                .divide(BigDecimal.valueOf(periodo), 8, RoundingMode.HALF_UP);

        var novaMediaPerda = mediaPerdaAnt.multiply(BigDecimal.valueOf(periodo - 1))
                .add(perda)
                .divide(BigDecimal.valueOf(periodo), 8, RoundingMode.HALF_UP);

        return calculateRsiFromAverages(novaMediaGanho, novaMediaPerda);
    }

    public static BigDecimal calcularSmaIncremental(BigDecimal precoAtual, BigDecimal somaAnterior,
                                                    BigDecimal precoMaisAntigoRemovido, int periodo) {
        var novaSoma = somaAnterior.subtract(precoMaisAntigoRemovido).add(precoAtual);
        return novaSoma.divide(BigDecimal.valueOf(periodo), 4, RoundingMode.HALF_UP);
    }

    public static BigDecimal calcularSma(List<BigDecimal> precos, int periodo) {
        if (precos.size() < periodo)
            return BigDecimal.ZERO;

        final var ultimosPrecos = precos.subList(precos.size() - periodo, precos.size());
        final var soma = ultimosPrecos.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        return soma.divide(BigDecimal.valueOf(periodo), 4, RoundingMode.HALF_UP);
    }

    public static BigDecimal somarLucros(List<BigDecimal> valoresCompra, BigDecimal valorAtualMoeda, BigDecimal valorOperacao) {
        var lucro = BigDecimal.ZERO;

        for (BigDecimal precoCompra : valoresCompra) {
            final var quantidade = valorOperacao.divide(precoCompra, 4, RoundingMode.HALF_UP);
            final var valorVenda = quantidade.multiply(valorAtualMoeda);

            lucro = lucro.add(valorVenda);
        }

        return lucro;
    }

    public static TriploRsiDTO calcularTriploRsi(List<CandleDTO> candles, int qntPeriodosRsiCurto,
                                                 int qntPeriodosRsiMedio, int qntPeriodosRsiLongo) {
        var somaGanhosCurto = BigDecimal.ZERO;
        var somaPerdasCurto = BigDecimal.ZERO;
        var somaGanhosMedio = BigDecimal.ZERO;
        var somaPerdasMedio = BigDecimal.ZERO;
        var somaGanhosLongo = BigDecimal.ZERO;
        var somaPerdasLongo = BigDecimal.ZERO;

        var rsiMedioSerie = new ArrayList<BigDecimal>();

        for (int i = 1; i <= qntPeriodosRsiLongo; i++) {
            var variacao = candles.get(i).valorFechamento().subtract(candles.get(i - 1).valorFechamento());

            if (variacao.compareTo(BigDecimal.ZERO) > 0) {
                if (i <= qntPeriodosRsiCurto)
                    somaGanhosCurto = somaGanhosCurto.add(variacao);

                if (i <= qntPeriodosRsiMedio)
                    somaGanhosMedio = somaGanhosMedio.add(variacao);

                somaGanhosLongo = somaGanhosLongo.add(variacao);
            } else {
                if (i <= qntPeriodosRsiCurto)
                    somaPerdasCurto = somaPerdasCurto.add(variacao.abs());

                if (i <= qntPeriodosRsiMedio)
                    somaPerdasMedio = somaPerdasMedio.add(variacao.abs());

                somaPerdasLongo = somaPerdasLongo.add(variacao.abs());
            }
        }

        var mediaGanhosCurto = somaGanhosCurto.divide(BigDecimal.valueOf(qntPeriodosRsiCurto), 4, RoundingMode.HALF_UP);
        var mediaPerdasCurto = somaPerdasCurto.divide(BigDecimal.valueOf(qntPeriodosRsiCurto), 4, RoundingMode.HALF_UP);
        var mediaGanhosMedio = somaGanhosMedio.divide(BigDecimal.valueOf(qntPeriodosRsiMedio), 4, RoundingMode.HALF_UP);
        var mediaPerdasMedio = somaPerdasMedio.divide(BigDecimal.valueOf(qntPeriodosRsiMedio), 4, RoundingMode.HALF_UP);
        var mediaGanhosLongo = somaGanhosLongo.divide(BigDecimal.valueOf(qntPeriodosRsiLongo), 4, RoundingMode.HALF_UP);
        var mediaPerdasLongo = somaPerdasLongo.divide(BigDecimal.valueOf(qntPeriodosRsiLongo), 4, RoundingMode.HALF_UP);

        rsiMedioSerie.add(calculateRsiFromAverages(mediaGanhosMedio, mediaPerdasMedio));

        for (int i = qntPeriodosRsiLongo + 1; i < candles.size(); i++) {
            var variacao = candles.get(i).valorFechamento().subtract(candles.get(i - 1).valorFechamento());
            var ganho = variacao.compareTo(BigDecimal.ZERO) > 0 ? variacao : BigDecimal.ZERO;
            var perda = variacao.compareTo(BigDecimal.ZERO) < 0 ? variacao.abs() : BigDecimal.ZERO;

            mediaGanhosCurto = calcularMediaGanhoRsi(mediaGanhosCurto, qntPeriodosRsiCurto, ganho);
            mediaPerdasCurto = calcularMediaPerdaRsi(mediaPerdasCurto, qntPeriodosRsiCurto, perda);

            mediaGanhosMedio = calcularMediaGanhoRsi(mediaGanhosMedio, qntPeriodosRsiMedio, ganho);
            mediaPerdasMedio = calcularMediaPerdaRsi(mediaPerdasMedio, qntPeriodosRsiMedio, perda);

            mediaGanhosLongo = calcularMediaGanhoRsi(mediaGanhosLongo, qntPeriodosRsiLongo, ganho);
            mediaPerdasLongo = calcularMediaPerdaRsi(mediaPerdasLongo, qntPeriodosRsiLongo, perda);

            rsiMedioSerie.add(calculateRsiFromAverages(mediaGanhosMedio, mediaPerdasMedio));
        }

        var rsiCurto = calculateRsiFromAverages(mediaGanhosCurto, mediaPerdasCurto);
        var rsiMedio = calculateRsiFromAverages(mediaGanhosMedio, mediaPerdasMedio);
        var rsiLongo = calculateRsiFromAverages(mediaGanhosLongo, mediaPerdasLongo);

        return new TriploRsiDTO(
                rsiCurto,
                rsiMedio,
                rsiLongo,
                rsiMedioSerie,
                mediaGanhosLongo,
                mediaPerdasLongo
        );
    }

    public static BigDecimal calcularMediaGanhoRsi(BigDecimal mediaGanhos, int qntPeriodos, BigDecimal ganho) {
        return (mediaGanhos.multiply(BigDecimal.valueOf(qntPeriodos - 1)).add(ganho))
                .divide(BigDecimal.valueOf(qntPeriodos), 4, RoundingMode.HALF_UP);
    }

    public static BigDecimal calcularMediaPerdaRsi(BigDecimal mediaPerdas, int qntPeriodos, BigDecimal perda) {
        return (mediaPerdas.multiply(BigDecimal.valueOf(qntPeriodos - 1)).add(perda))
                .divide(BigDecimal.valueOf(qntPeriodos), 4, RoundingMode.HALF_UP);
    }

    public static ResultadoRsiEstocasticoDTO calcularRsiEstocasticoDeSerie(List<BigDecimal> rsiMedioSerie, int periodoEstocastico,
                                                                           int suavizacaoK, int suavizacaoD) {
        List<BigDecimal> kValues = new ArrayList<>();

        for (int i = periodoEstocastico - 1; i < rsiMedioSerie.size(); i++) {
            var window = rsiMedioSerie.subList(i - periodoEstocastico + 1, i + 1);
            var min = window.stream().min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
            var max = window.stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
            var k = (max.subtract(min).compareTo(BigDecimal.ZERO) == 0)
                    ? BigDecimal.ZERO
                    : rsiMedioSerie.get(i).subtract(min).divide(max.subtract(min), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

            kValues.add(k);
        }

        var smoothKValues = sma(kValues, suavizacaoK);
        var smoothDValues = sma(smoothKValues, suavizacaoD);
        var ultimoK = smoothKValues.getLast();
        var penultimoK = smoothKValues.get(smoothKValues.size() - 2);
        var ultimoD = smoothDValues.getLast();
        var penultimoD = smoothDValues.get(smoothDValues.size() - 2);

        return new ResultadoRsiEstocasticoDTO(ultimoK, penultimoK, ultimoD, penultimoD);
    }

    public static BigDecimal calculateRsiFromAverages(BigDecimal avgGanhos, BigDecimal avgPerdas) {
        if (avgPerdas.compareTo(BigDecimal.ZERO) == 0)
            return BigDecimal.valueOf(100);

        var rs = avgGanhos.divide(avgPerdas, 4, RoundingMode.HALF_UP);

        return BigDecimal.valueOf(100).subtract(BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), 4, RoundingMode.HALF_UP));
    }

    private static List<BigDecimal> sma(List<BigDecimal> values, int length) {
        var result = new ArrayList<BigDecimal>();

        for (int i = length - 1; i < values.size(); i++) {
            var sum = BigDecimal.ZERO;

            for (int j = i - length + 1; j <= i; j++) {
                sum = sum.add(values.get(j));
            }

            result.add(sum.divide(BigDecimal.valueOf(length), 4, RoundingMode.HALF_UP));
        }

        return result;
    }

}