import type { GridColDef, GridRenderCellParams } from '@mui/x-data-grid';
import { statusOperacoesLabels } from '../../utils/enumMappings';
import type { StatusOperacoes } from '../../types/operacao';
import { StartStopActionRenderer } from '../../utils/gridRenderers';

export const operacaoColumns: GridColDef[] = [
    {
        field: 'status',
        headerName: 'Status',
        width: 130,
        resizable: false,
        valueGetter: (value: StatusOperacoes) => statusOperacoesLabels[value] || value,
    },
    {
        field: 'controle',
        headerName: 'Ações',
        width: 100,
        sortable: false,
        renderCell: StartStopActionRenderer
    },
    { field: 'par', headerName: 'Par', width: 120, resizable: false },
    { field: 'intervalo', headerName: 'Intervalo', width: 90, resizable: false },
    {
        field: 'modoTeste',
        headerName: 'Modo',
        width: 100,
        renderCell: (params: GridRenderCellParams) => (
            <span style={{
                color: params.value ? '#ff9800' : '#4caf50',
                fontWeight: 'bold',
                padding: '4px 8px',
                borderRadius: '4px',
                background: params.value ? 'rgba(255, 152, 0, 0.1)' : 'rgba(76, 175, 80, 0.1)',
                fontSize: '0.75rem'
            }}>
                {params.value ? 'TESTE' : 'REAL'}
            </span>
        )
    },
    {
        field: 'saldo',
        headerName: 'Saldo (USDT)',
        width: 120,
        valueGetter: (_value: any, row: any) => {
            if (row.modoTeste) {
                return row.saldoInicial != null
                    ? `$ ${Number(row.saldoInicial).toFixed(2)}`
                    : '$ 0.00';
            }
            return '-';
        }
    },
    {
        field: 'operador',
        headerName: 'Operador',
        flex: 1,
        minWidth: 150,
        resizable: false,
        valueGetter: (value: { nome: string }) => value?.nome || '-',
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