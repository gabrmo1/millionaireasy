export interface CandleChartDTO {
    time: number;
    open: number;
    high: number;
    low: number;
    close: number;
    volume: number;
}

export interface EventoChartDTO {
    time: number;
    tipo: 'COMPRA' | 'VENDA';
    preco: number;
    tooltip: string;
    cor: string;
}

export interface IndicadorPointDTO {
    time: number;
    value: number;
}

export interface MonitoramentoDataDTO {
    par: string;
    intervalo: string;
    candles: CandleChartDTO[];
    eventos: EventoChartDTO[];

    // Chave: Nome do indicador (ex: "RSI_Curto", "EMA"), Valor: Lista de pontos
    indicadores: Record<string, IndicadorPointDTO[]>;
}