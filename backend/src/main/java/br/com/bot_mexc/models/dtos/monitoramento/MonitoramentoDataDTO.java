package br.com.bot_mexc.models.dtos.monitoramento;

import lombok.Builder;
import java.util.List;
import java.util.Map;

@Builder
public record MonitoramentoDataDTO(
        String par,
        String intervalo,
        List<CandleChartDTO> candles,
        List<EventoChartDTO> eventos,

        // Chave = Nome do Indicador (ex: "RSI_Curto"), Valor = Lista de pontos
        Map<String, List<IndicadorPointDTO>> indicadores
) {}