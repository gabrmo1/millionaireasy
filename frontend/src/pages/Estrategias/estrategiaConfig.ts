import type { GridColDef } from '@mui/x-data-grid';
import { renderBooleanCell } from '../../utils/gridRenderers';
import type { FormField } from '../../types/common';

export const estrategiaGridColumns: GridColDef[] = [
    { field: 'nome', headerName: 'Nome', flex: 1, minWidth: 200, resizable: false },
    { field: 'utilizarRsiCurto', headerName: 'RSI Curto?', width: 130, renderCell: renderBooleanCell, resizable: false },
    { field: 'utilizarRsiMedio', headerName: 'RSI Médio?', width: 130, renderCell: renderBooleanCell, resizable: false },
    { field: 'utilizarRsiLongo', headerName: 'RSI Longo?', width: 130, renderCell: renderBooleanCell, resizable: false },
    { field: 'utilizarRsiEstocastico', headerName: 'RSI Estocástico?', width: 150, renderCell: renderBooleanCell, resizable: false },
    { field: 'utilizarEma', headerName: 'Usa EMA?', width: 120, renderCell: renderBooleanCell, resizable: false },
];

export const estrategiaFormConfig: FormField<any>[] = [
    { name: 'nome', label: 'Nome da Estratégia', type: 'text', required: true, gridSpan: 4, validation: {
            maxLength: { value: 50, message: 'O nome não pode exceder 50 caracteres.' }
        }
    },
    { name: 'utilizarRsiCurto', label: 'Utilizar RSI Curto', type: 'checkbox', defaultValue: false, gridSpan: 12 },
    { name: 'periodoRsiCurto', label: 'Período', type: 'number', defaultValue: 7, gridSpan: 2, dependentOn: 'utilizarRsiCurto', validation: { min: { value: 1, message: 'O período deve ser positivo.' } } },
    { name: 'utilizarRsiMedio', label: 'Utilizar RSI Médio', type: 'checkbox', defaultValue: false, gridSpan: 12 },
    { name: 'periodoRsiMedio', label: 'Período', type: 'number', defaultValue: 14, gridSpan: 2, dependentOn: 'utilizarRsiMedio', validation: { min: { value: 1, message: 'O período deve ser positivo.' } } },
    { name: 'utilizarRsiLongo', label: 'Utilizar RSI Longo', type: 'checkbox', defaultValue: false, gridSpan: 12 },
    { name: 'periodoRsiLongo', label: 'Período', type: 'number', defaultValue: 21, gridSpan: 2, dependentOn: 'utilizarRsiLongo', validation: { min: { value: 1, message: 'O período deve ser positivo.' } } },
    { name: 'utilizarRsiEstocastico', label: 'Utilizar RSI Estocástico', type: 'checkbox', defaultValue: false, gridSpan: 12 },
    { name: 'periodoRsiEstocastico', label: 'Período', type: 'number', defaultValue: 14, gridSpan: 2, dependentOn: 'utilizarRsiEstocastico', validation: { min: { value: 1, message: 'O período deve ser positivo.' } } },
    { name: 'suavizacaoRsiEstocasticoK', label: 'Suav. %K', type: 'number', defaultValue: 3, gridSpan: 2, dependentOn: 'utilizarRsiEstocastico', validation: { min: { value: 1, message: 'A suavização deve ser positiva.' } } },
    { name: 'suavizacaoRsiEstocasticoD', label: 'Suav. %D', type: 'number', defaultValue: 3, gridSpan: 2, dependentOn: 'utilizarRsiEstocastico', validation: { min: { value: 1, message: 'A suavização deve ser positiva.' } } },
    { name: 'utilizarEma', label: 'Utilizar EMA', type: 'checkbox', defaultValue: false, gridSpan: 12 },
    { name: 'periodoEma', label: 'Período', type: 'number', defaultValue: 200, gridSpan: 2, dependentOn: 'utilizarEma', validation: { max: { value: 200, message: 'O período EMA não pode ser maior que 200.' }, min: { value: 1, message: 'O período deve ser positivo.' } } },
    { name: 'utilizarSma', label: 'Utilizar SMA', type: 'checkbox', defaultValue: false, gridSpan: 12 },
    { name: 'periodoSma', label: 'Período', type: 'number', defaultValue: 200, gridSpan: 2, dependentOn: 'utilizarSma', validation: { max: { value: 200, message: 'O período SMA não pode ser maior que 200.' }, min: { value: 1, message: 'O período deve ser positivo.' } } },
    { name: 'realizarLeituraVolume', label: 'Realizar Leitura de Volume', type: 'checkbox', defaultValue: false, gridSpan: 12 },
];