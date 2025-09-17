import type { GridColDef } from '@mui/x-data-grid';
import { renderBooleanCell } from '../../utils/gridRenderers';
import type { FormField } from '../../types/common';
import { getPosicaoFaixasOptions } from '../../utils/enumMappings';

export const estrategiaGridColumns: GridColDef[] = [
    { field: 'nome', headerName: 'Nome', flex: 1, minWidth: 200 },
    { field: 'utilizarRsiCurto', headerName: 'RSI Curto?', width: 130, renderCell: renderBooleanCell },
    { field: 'utilizarRsiMedio', headerName: 'RSI Médio?', width: 130, renderCell: renderBooleanCell },
    { field: 'utilizarRsiLongo', headerName: 'RSI Longo?', width: 130, renderCell: renderBooleanCell },
    { field: 'utilizarRsiEstocastico', headerName: 'RSI Estocástico?', width: 150, renderCell: renderBooleanCell },
    { field: 'utilizarEma', headerName: 'Usa EMA?', width: 120, renderCell: renderBooleanCell },
];

export const estrategiaFormConfig: FormField<any>[] = [
    { name: 'nome', label: 'Nome da Estratégia', type: 'text', required: true, gridSpan: 4, validation: { maxLength: { value: 50, message: 'O nome não pode exceder 50 caracteres.' } } },
    { name: 'utilizarRsiCurto', label: 'Utilizar RSI Curto', type: 'checkbox', defaultValue: false, gridSpan: 12 },
    { name: 'periodoRsiCurto', label: 'Período', type: 'number', defaultValue: 7, gridSpan: 2, dependentOn: 'utilizarRsiCurto' },
    { name: 'utilizarRsiMedio', label: 'Utilizar RSI Médio', type: 'checkbox', defaultValue: false, gridSpan: 12 },
    { name: 'periodoRsiMedio', label: 'Período', type: 'number', defaultValue: 14, gridSpan: 2, dependentOn: 'utilizarRsiMedio' },
    { name: 'utilizarRsiLongo', label: 'Utilizar RSI Longo', type: 'checkbox', defaultValue: false, gridSpan: 12 },
    { name: 'periodoRsiLongo', label: 'Período', type: 'number', defaultValue: 21, gridSpan: 2, dependentOn: 'utilizarRsiLongo' },
    { name: 'utilizarRsiEstocastico', label: 'Utilizar RSI Estocástico', type: 'checkbox', defaultValue: false, gridSpan: 12 },
    { name: 'periodoRsiEstocastico', label: 'Período', type: 'number', defaultValue: 14, gridSpan: 2, dependentOn: 'utilizarRsiEstocastico' },
    { name: 'suavizacaoRsiEstocasticoK', label: 'Suav. %K', type: 'number', defaultValue: 3, gridSpan: 2, dependentOn: 'utilizarRsiEstocastico' },
    { name: 'suavizacaoRsiEstocasticoD', label: 'Suav. %D', type: 'number', defaultValue: 3, gridSpan: 2, dependentOn: 'utilizarRsiEstocastico' },
    { name: 'utilizarEma', label: 'Utilizar EMA', type: 'checkbox', defaultValue: false, gridSpan: 12 },
    { name: 'periodoEma', label: 'Período', type: 'number', defaultValue: 200, gridSpan: 2, dependentOn: 'utilizarEma', validation: { max: { value: 200, message: 'O período EMA não pode ser maior que 200.' } } },
    { name: 'utilizarSma', label: 'Utilizar SMA', type: 'checkbox', defaultValue: false, gridSpan: 12 },
    { name: 'periodoSma', label: 'Período', type: 'number', defaultValue: 200, gridSpan: 2, dependentOn: 'utilizarSma', validation: { max: { value: 200, message: 'O período SMA não pode ser maior que 200.' } } },
    { name: 'realizarLeituraVolume', label: 'Realizar Leitura de Volume', type: 'checkbox', defaultValue: false, gridSpan: 12 },
];

const posicaoOptions = getPosicaoFaixasOptions();

export const condicaoCompraFormFields: FormField<any>[] = [
    { name: 'valorOperacaoFixo', label: 'Valor Fixo (Ex: 10.50)', type: 'number', gridSpan: 6 },
    { name: 'percentualValorOperacao', label: 'Percentual do Saldo (Ex: 5)', type: 'number', gridSpan: 6 },
    { name: 'posicaoFaixaRsiCurto', label: 'Posição RSI Curto', type: 'select', options: posicaoOptions, gridSpan: 6 },
    { name: 'faixaRsiCurto', label: 'Faixa RSI Curto', type: 'number', gridSpan: 6 },
];

export const condicaoVendaFormFields: FormField<any>[] = [
    { name: 'quantiaSobreLucro', label: 'Percentual de Lucro', type: 'number', gridSpan: 12 },
    { name: 'posicaoFaixaVendaRsiCurto', label: 'Posição RSI Curto', type: 'select', options: posicaoOptions, gridSpan: 6 },
    { name: 'faixaVendaRsiCurto', label: 'Faixa RSI Curto', type: 'number', gridSpan: 6 },
];