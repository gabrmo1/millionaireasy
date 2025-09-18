import type { GridColDef } from '@mui/x-data-grid';
import { statusOperacoesLabels } from '../../utils/enumMappings';
import type { StatusOperacoes } from '../../types/operacao';

export const operacaoGridColumns: GridColDef[] = [
    { field: 'par', headerName: 'Par', width: 120, resizable: false },
    { field: 'intervalo', headerName: 'Intervalo', width: 90, resizable: false },
    {
        field: 'status',
        headerName: 'Status',
        width: 130,
        resizable: false,
        valueGetter: (value: StatusOperacoes) => statusOperacoesLabels[value] || value,
    },
    {
        field: 'operador',
        headerName: 'Operador',
        flex: 1,
        minWidth: 150,
        resizable: false,
        valueGetter: (value: { nome: string }) => value?.nome || '',
    },
    {
        field: 'estrategia',
        headerName: 'Estratégia',
        flex: 1,
        minWidth: 200,
        resizable: false,
        valueGetter: (value: { nome: string }) => value?.nome || '',
    },
];