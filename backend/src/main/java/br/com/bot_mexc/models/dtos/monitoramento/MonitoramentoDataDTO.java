package br.com.bot_mexc.models.dtos.monitoramento;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record MonitoramentoDataDTO(
        String par,
        String intervalo,
        String nomeEstrategia,
        BigDecimal lucroTotal,
        List<CandleChartDTO> candles,
        List<EventoChartDTO> eventos,

        // Chave = Nome do Indicador (ex: "RSI_Curto"), Valor = Lista de pontos
        Map<String, List<IndicadorPointDTO>> indicadores
) {
}