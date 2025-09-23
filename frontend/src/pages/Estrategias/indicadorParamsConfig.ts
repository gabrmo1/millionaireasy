import { TipoIndicador } from '../../types/enums';

type ParamConfig = {
    key: string;
    label: string;
    defaultValue: number;
    min?: number;
    max?: number;
};

// Mapeia cada tipo de indicador aos seus parâmetros configuráveis
export const indicadorParamsConfig: Partial<Record<TipoIndicador, ParamConfig[]>> = {
    [TipoIndicador.RSI_CURTO]: [
        { key: 'periodoRsiCurto', label: 'Período', defaultValue: 7, min: 1 },
    ],
    [TipoIndicador.RSI_MEDIO]: [
        { key: 'periodoRsiMedio', label: 'Período', defaultValue: 14, min: 1 },
    ],
    [TipoIndicador.RSI_LONGO]: [
        { key: 'periodoRsiLongo', label: 'Período', defaultValue: 21, min: 1 },
    ],
    [TipoIndicador.RSI_ESTOCASTICO_K]: [
        { key: 'periodoRsiEstocastico', label: 'Período RSI', defaultValue: 14, min: 1 },
        { key: 'suavizacaoRsiEstocasticoK', label: 'Suavização %K', defaultValue: 3, min: 1 },
    ],
    [TipoIndicador.RSI_ESTOCASTICO_D]: [
        { key: 'periodoRsiEstocastico', label: 'Período RSI', defaultValue: 14, min: 1 },
        { key: 'suavizacaoRsiEstocasticoK', label: 'Suavização %K', defaultValue: 3, min: 1 },
        { key: 'suavizacaoRsiEstocasticoD', label: 'Suavização %D', defaultValue: 3, min: 1 },
    ],
    [TipoIndicador.EMA]: [
        { key: 'periodoEma', label: 'Período', defaultValue: 200, min: 1, max: 200 },
    ],
    [TipoIndicador.SMA]: [
        { key: 'periodoSma', label: 'Período', defaultValue: 200, min: 1, max: 200 },
    ],
    // VOLUME não possui parâmetros configuráveis
};