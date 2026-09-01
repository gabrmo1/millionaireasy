import React, { useState, useCallback, useMemo } from 'react';
import type { GridColDef, GridRenderCellParams } from '@mui/x-data-grid';
import { IconButton, Tooltip } from '@mui/material';
import AnalyticsIcon from '@mui/icons-material/Analytics';
import GenericCrudPage from '../../components/common/pages/GenericCrudPage';
import MonitoramentoModal from '../../components/modals/MonitoramentoModal';
import SimulacaoForm from './SimulacaoForm';
import { getSimulacoes } from '../../services/simulacaoService';
import { StatusChipRenderer } from '../../utils/gridRenderers';
import type { Simulacao } from '../../types/simulacao';

const SimulacoesPage: React.FC = () => {
    const [monitorModalOpen, setMonitorModalOpen] = useState(false);
    const [selectedSimulacaoId, setSelectedSimulacaoId] = useState<string | null>(null);

    // Ocultar a abertura do modal em um callback para estabilidade de referência
    const handleOpenMonitor = useCallback((id: string) => {
        setSelectedSimulacaoId(id);
        setMonitorModalOpen(true);
    }, []);

    // A definição de colunas agora é memoizada. Isso impede que o DynamicDataGrid
    // faça re-diffing das colunas a cada tick do polling.
    const columns: GridColDef<Simulacao>[] = useMemo(() => [
        { field: 'par', headerName: 'Par', width: 130 },
        { field: 'intervalo', headerName: 'Intervalo', width: 100 },
        {
            field: 'estrategia',
            headerName: 'Estratégia',
            flex: 1,
            valueGetter: (value: { nome: string } | undefined) => value?.nome || 'N/A'
        },
        { field: 'saldoInicial', headerName: 'Saldo Inicial ($)', width: 130 },
        { field: 'dataInicio', headerName: 'Data Início', width: 180 },
        { field: 'dataFim', headerName: 'Data Fim', width: 180 },
        { field: 'status', headerName: 'Status', width: 140, renderCell: StatusChipRenderer },
        {
            field: 'acoes',
            headerName: 'Resultado',
            width: 100,
            sortable: false,
            renderCell: (params: GridRenderCellParams<Simulacao>) => (
                <Tooltip title="Ver Resultados">
                    <span>
                        <IconButton
                            size="small"
                            color="primary"
                            disabled={params.row.status !== 'FINALIZADO'}
                            onClick={() => handleOpenMonitor(params.row.id)}
                        >
                            <AnalyticsIcon />
                        </IconButton>
                    </span>
                </Tooltip>
            )
        }
    ], [handleOpenMonitor]);

    // Condição de performance: só realizar chamadas HTTP de polling
    // se existir alguma simulação que justifique a consulta.
    const shouldPoll = useCallback((items: Simulacao[]) => {
        return items.some(i => i.status === 'AGUARDANDO' || i.status === 'EM_ANDAMENTO');
    }, []);

    return (
        <>
            <GenericCrudPage<Simulacao>
                title="Simulações (Backtesting)"
                description="Inicie e acompanhe o processamento de simulações em segundo plano."
                fetcher={getSimulacoes}
                gridColumns={columns}
                FormComponent={SimulacaoForm}
                pollingInterval={5000} // Consulta a cada 5 segundos
                pollingCondition={shouldPoll}
            />

            <MonitoramentoModal
                open={monitorModalOpen}
                onClose={() => setMonitorModalOpen(false)}
                operacaoId={selectedSimulacaoId}
            />
        </>
    );
};

export default SimulacoesPage;