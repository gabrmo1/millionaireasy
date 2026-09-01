package br.com.bot_mexc.models.dtos;

import java.util.List;

public record CandlePersistPayloadDTO(
        String par,
        String intervalo,
        List<CandleDTO> candles
) {}