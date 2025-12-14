package br.com.bot_mexc.utils;

import br.com.bot_mexc.models.dtos.CandleDTO;
import br.com.bot_mexc.models.entities.Candle;
import br.com.bot_mexc.models.entities.Operacao;
import lombok.experimental.UtilityClass;
import org.json.JSONArray;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class CandleUtils {

    public static List<CandleDTO> montarCandles(String response) {
        JSONArray arr = new JSONArray(response);
        List<CandleDTO> candles = new ArrayList<>();

        for (int i = 0; i < arr.length(); i++) {
            JSONArray candleJson = arr.getJSONArray(i);
            ZoneId zonaBrasil = ZoneId.of("America/Sao_Paulo");
            LocalDateTime openTime = Instant.ofEpochMilli(candleJson.getLong(0)).atZone(zonaBrasil).toLocalDateTime();
            LocalDateTime closeTime = Instant.ofEpochMilli(candleJson.getLong(6)).atZone(zonaBrasil).toLocalDateTime();
            BigDecimal open = new BigDecimal(candleJson.getString(1));
            BigDecimal high = new BigDecimal(candleJson.getString(2));
            BigDecimal low = new BigDecimal(candleJson.getString(3));
            BigDecimal close = new BigDecimal(candleJson.getString(4));
            BigDecimal volume = new BigDecimal(candleJson.getString(5));

            candles.add(new CandleDTO(openTime, closeTime, open, close, low, high, volume));
        }

        return candles;
    }

    public static Candle converterDtoParaEntidade(CandleDTO dto, String par, String intervalo) {
        return Candle.builder()
                .par(par)
                .intervalo(intervalo)
                .dataAbertura(dto.dataAbertura())
                .dataFechamento(dto.dataFechamento())
                .minima(dto.minima())
                .maxima(dto.maxima())
                .valorAbertura(dto.valorAbertura())
                .valorFechamento(dto.valorFechamento())
                .volume(dto.volume())
                .build();
    }

    public static CandleDTO converterEntidadeParaDto(Candle entidade) {
        return new CandleDTO(
                entidade.getDataAbertura(),
                entidade.getDataFechamento(),
                entidade.getValorAbertura(),
                entidade.getValorFechamento(),
                entidade.getMinima(),
                entidade.getMaxima(),
                entidade.getVolume()
        );
    }

    public static List<String> montarParesDeBusca(List<Operacao> operacoes) {
        return operacoes.stream()
                .map(op -> op.getPar() + "," + op.getIntervalo())
                .distinct()
                .toList();
    }
}