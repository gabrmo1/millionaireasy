package br.com.bot_mexc.utils;

import br.com.bot_mexc.models.dtos.CandleDTO;
import br.com.bot_mexc.models.dtos.mexc.EventoCandleMexcDTO;
import br.com.bot_mexc.models.entities.Candle;
import br.com.bot_mexc.proto.PublicSpotKlineV3Api;
import lombok.experimental.UtilityClass;
import org.json.JSONArray;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class CandleUtils {

    public static List<CandleDTO> buildListCandleDtoFromMexcResponse(String response) {
        JSONArray arr = new JSONArray(response);
        List<CandleDTO> candles = new ArrayList<>();

        for (int i = 0; i < arr.length(); i++) {
            JSONArray candleJson = arr.getJSONArray(i);
            long openTime = candleJson.getLong(0) / 1000L;
            long closeTime = candleJson.getLong(6) / 1000L;
            BigDecimal open = new BigDecimal(candleJson.getString(1));
            BigDecimal high = new BigDecimal(candleJson.getString(2));
            BigDecimal low = new BigDecimal(candleJson.getString(3));
            BigDecimal close = new BigDecimal(candleJson.getString(4));
            BigDecimal volume = new BigDecimal(candleJson.getString(5));

            candles.add(new CandleDTO(openTime, closeTime, open, close, low, high, volume));
        }

        return candles;
    }

    public static Candle buildEntityFromDto(CandleDTO dto, String par, String intervalo) {
        return Candle.builder()
                .par(par)
                .intervalo(intervalo)
                .dataAbertura(Instant.ofEpochSecond(dto.dataAbertura()))
                .dataFechamento(Instant.ofEpochSecond(dto.dataFechamento()))
                .minima(dto.minima())
                .maxima(dto.maxima())
                .valorAbertura(dto.valorAbertura())
                .valorFechamento(dto.valorFechamento())
                .volume(dto.volume())
                .build();
    }

    public static CandleDTO buildDtoFromEntity(Candle entidade) {
        return new CandleDTO(
                entidade.getDataAbertura().getEpochSecond(),
                entidade.getDataFechamento().getEpochSecond(),
                entidade.getValorAbertura(),
                entidade.getValorFechamento(),
                entidade.getMinima(),
                entidade.getMaxima(),
                entidade.getVolume()
        );
    }



    public static EventoCandleMexcDTO mapProtoToDto(PublicSpotKlineV3Api proto, String symbol) {
        return new EventoCandleMexcDTO(
                symbol,
                proto.getInterval(),
                proto.getWindowStart(),
                proto.getWindowEnd(),
                new BigDecimal(proto.getOpeningPrice()),
                new BigDecimal(proto.getClosingPrice()),
                new BigDecimal(proto.getHighestPrice()),
                new BigDecimal(proto.getLowestPrice()),
                new BigDecimal(proto.getVolume()),
                new BigDecimal(proto.getAmount())
        );
    }
}