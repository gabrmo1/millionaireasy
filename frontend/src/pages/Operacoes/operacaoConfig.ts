import type { GridColDef } from '@mui/x-data-grid';
import { statusOperacoesLabels } from '../../utils/enumMappings';
import type { StatusOperacoes } from '../../types/operacao';

export const operacaoGridColumns: GridColDef[] = [
    { field: 'par', headerName: 'Par', width: 120 },
    { field: 'intervalo', headerName: 'Intervalo', width: 90 },
    {
        field: 'status',
        headerName: 'Status',
        width: 130,
        valueGetter: (value: StatusOperacoes) => statusOperacoesLabels[value] || value,
    },
    {
        field: 'operador',
        headerName: 'Operador',
        flex: 1,
        minWidth: 150,
        valueGetter: (value: { nome: string }) => value?.nome || '',
    },
    {
        field: 'estrategia',
        headerName: 'Estratégia',
        flex: 1,
        minWidth: 200,
        valueGetter: (value: { nome: string }) => value?.nome || '',
    },
];