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

export const TipoMoedaValorOperacao = {
    BASE: 'BASE',
    QUOTE: 'QUOTE'
} as const;

export type TipoMoedaValorOperacao = typeof TipoMoedaValorOperacao[keyof typeof TipoMoedaValorOperacao];