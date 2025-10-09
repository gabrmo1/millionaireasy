import { TipoIndicador } from '../../types/enums';

type ParamConfig = {
    key: string;
    label: string;
    defaultValue: number;
    required: boolean;
    min?: number;
    max?: number;
};

export const indicadorParamsConfig: Partial<Record<TipoIndicador, ParamConfig[]>> = {
    [TipoIndicador.RSI_CURTO]: [
        { key: 'periodoRsiCurto', label: 'Período', defaultValue: 7, required: true, min: 4, max: 10 },
    ],
    [TipoIndicador.RSI_MEDIO]: [
        { key: 'periodoRsiMedio', label: 'Período', defaultValue: 14, required: true, min: 11, max: 17 },
    ],
    [TipoIndicador.RSI_LONGO]: [
        { key: 'periodoRsiLongo', label: 'Período', defaultValue: 21, required: true, min: 18, max: 24 },
    ],
    [TipoIndicador.RSI_ESTOCASTICO_K]: [
        { key: 'periodoRsiEstocastico', label: 'Período RSI', defaultValue: 14, required: true, min: 4, max: 24 },
        { key: 'suavizacaoRsiEstocasticoK', label: 'Suavização %K', defaultValue: 3, required: true, min: 1 },
    ],
    [TipoIndicador.RSI_ESTOCASTICO_D]: [
        { key: 'periodoRsiEstocastico', label: 'Período RSI', defaultValue: 14, required: true, min: 4, max: 24 },
        { key: 'suavizacaoRsiEstocasticoK', label: 'Suavização %K', defaultValue: 3, required: true, min: 1 },
        { key: 'suavizacaoRsiEstocasticoD', label: 'Suavização %D', defaultValue: 3, required: true, min: 1 },
    ],
    [TipoIndicador.EMA]: [
        { key: 'periodoEma', label: 'Período', defaultValue: 200, required: true, min: 1, max: 200 },
    ],
    [TipoIndicador.SMA]: [
        { key: 'periodoSma', label: 'Período', defaultValue: 200, required: true, min: 1, max: 200 },
    ],
};