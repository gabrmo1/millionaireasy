import type { GridColDef, GridRenderCellParams } from '@mui/x-data-grid';
import { statusOperacoesLabels } from '../../utils/enumMappings';
import type { StatusOperacoes, Operacao } from '../../types/operacao';
import { StartStopAction } from '../../utils/gridRenderers';
import AnalyticsIcon from '@mui/icons-material/Analytics';
import { IconButton, Tooltip } from '@mui/material';

export const getOperacaoColumns = (
    onRefresh: () => void,
    onOpenMonitor: (operacao: Operacao) => void
): GridColDef[] => [
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
        width: 140,
        sortable: false,
        renderCell: (params: GridRenderCellParams) => (
            <div style={{ display: 'flex', gap: '4px' }}>
                <StartStopAction params={params} onRefresh={onRefresh} />
                <Tooltip title="Monitorar e Analisar">
                    <IconButton
                        size="small"
                        color="primary"
                        onClick={() => onOpenMonitor(params.row)}
                    >
                        <AnalyticsIcon />
                    </IconButton>
                </Tooltip>
            </div>
        )
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

export const operacaoColumns: GridColDef[] = getOperacaoColumns(() => {}, () => {});