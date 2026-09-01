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

export interface RelatorioDesempenhoDTO {
    dataInicio: string | null;
    dataFim: string | null;

    saldoLiquidoTotal: number | null;
    lucroBruto: number | null;
    fatorLucro: number | null;
    mediaLucroPrejuizo: number | null;

    maiorOperacaoVencedora: number | null;
    mediaOperacoesVencedoras: number | null;
    operacoesVencedoras: number | null;
    maiorSequenciaVencedora: number | null;
    totalOperacoes: number | null;
    percentualOperacoesVencedoras: number | null;
    mediaTempoOperacoesVencedoras: string | null;
    mediaTempoOperacoes: string | null;

    declinioMaximoValor: number | null;
    declinioMaximoPercentual: number | null;

    saldoTotal: number | null;
    prejuizoBruto: number | null;
    retornoCapitalInicial: number | null;
    custos: number | null;

    maiorOperacaoPerdedora: number | null;
    mediaOperacoesPerdedoras: number | null;
    operacoesPerdedoras: number | null;
    maiorSequenciaPerdedora: number | null;
    operacoesZeradas: number | null;
    percentualOperacoesPerdedoras: number | null;
    mediaTempoOperacoesPerdedoras: string | null;

    maeValor: number | null;
    maePercentual: number | null;
}