import type { FormField } from '../../types/common';
import { getOperadores } from '../../services/operadorService';
import { getEstrategias } from '../../services/estrategiaService';

export const operacaoFormConfig: FormField<any>[] = [
    {
        name: 'par', label: 'Par de Moedas (ex: BTCUSDT)', type: 'text', required: true, gridSpan: 6,
        validation: { maxLength: { value: 20, message: 'O par não pode exceder 20 caracteres.' } }
    },
    {
        name: 'intervalo', label: 'Intervalo (ex: 15m)', type: 'text', required: true, gridSpan: 6,
        validation: { maxLength: { value: 5, message: 'O intervalo não pode exceder 5 caracteres.' } }
    },
    {
        name: 'idOperador',
        label: 'Operador',
        type: 'entitySelector',
        required: true,
        gridSpan: 6,
        modalConfig: {
            title: 'Selecione um Operador',
            fetcher: getOperadores,
            displayAttribute: 'nome',
        },
    },
    {
        name: 'idEstrategia',
        label: 'Estratégia',
        type: 'entitySelector',
        required: true,
        gridSpan: 6,
        modalConfig: {
            title: 'Selecione uma Estratégia',
            fetcher: getEstrategias,
            displayAttribute: 'nome',
        },
    },
];