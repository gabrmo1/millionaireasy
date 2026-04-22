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
    nomeEstrategia: string;
    lucroTotal: number;
    candles: CandleChartDTO[];
    eventos: EventoChartDTO[];
    indicadores: Record<string, IndicadorPointDTO[]>;
}