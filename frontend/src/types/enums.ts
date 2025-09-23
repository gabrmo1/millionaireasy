export const PosicaoFaixasCompraVenda = {
    ABAIXO: 'ABAIXO',
    ACIMA: 'ACIMA',
    CRUZAR_PARA_BAIXO: 'CRUZAR_PARA_BAIXO',
    CRUZAR_PARA_CIMA: 'CRUZAR_PARA_CIMA'
} as const;
export type PosicaoFaixasCompraVenda = typeof PosicaoFaixasCompraVenda[keyof typeof PosicaoFaixasCompraVenda];

export const TipoIndicador = {
    RSI_CURTO: 'RSI_CURTO',
    RSI_MEDIO: 'RSI_MEDIO',
    RSI_LONGO: 'RSI_LONGO',
    RSI_ESTOCASTICO_K: 'RSI_ESTOCASTICO_K',
    RSI_ESTOCASTICO_D: 'RSI_ESTOCASTICO_D',
    EMA: 'EMA',
    SMA: 'SMA',
    VOLUME: 'VOLUME'
} as const;
export type TipoIndicador = typeof TipoIndicador[keyof typeof TipoIndicador];

export const OperadorLogico = {
    AND: 'AND',
    OR: 'OR'
} as const;
export type OperadorLogico = typeof OperadorLogico[keyof typeof OperadorLogico];

export const TipoOperando = {
    INDICADOR: 'INDICADOR',
    PRECO_FECHAMENTO: 'PRECO_FECHAMENTO',
    VALOR_FIXO: 'VALOR_FIXO'
} as const;
export type TipoOperando = typeof TipoOperando[keyof typeof TipoOperando];

export const OperadorComparacao = {
    CRUZOU_PARA_CIMA: 'CRUZOU_PARA_CIMA',
    CRUZOU_PARA_BAIXO: 'CRUZOU_PARA_BAIXO',
    MAIOR_QUE: 'MAIOR_QUE',
    MENOR_QUE: 'MENOR_QUE'
} as const;
export type OperadorComparacao = typeof OperadorComparacao[keyof typeof OperadorComparacao];