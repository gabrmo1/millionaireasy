import type { GridColDef } from '@mui/x-data-grid';

export const estrategiaGridColumns: GridColDef[] = [
    { field: 'nome', headerName: 'Nome da Estratégia', flex: 1, minWidth: 250, resizable: false },
    {
        field: 'indicadoresConfig',
        headerName: 'Indicadores',
        flex: 1,
        minWidth: 150,
        resizable: false,
        valueGetter: (value: any[]) => value ? value.length : 0,
    },
    {
        field: 'condicoesCompra',
        headerName: 'Regras de Compra',
        flex: 1,
        minWidth: 150,
        resizable: false,
        valueGetter: (value: any[]) => value ? value.length : 0,
    },
    {
        field: 'condicoesVenda',
        headerName: 'Regras de Venda',
        flex: 1,
        minWidth: 150,
        resizable: false,
        valueGetter: (value: any[]) => value ? value.length : 0,
    }
];