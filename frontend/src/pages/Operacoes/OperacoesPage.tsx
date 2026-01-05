import React, { useState } from 'react';
import GenericCrudPage from '../../components/common/pages/GenericCrudPage';
import { getOperacoes, deleteOperacao } from '../../services/operacaoService';
import { getOperacaoColumns } from './operacaoConfig.tsx';
import type { Operacao } from '../../types/operacao';
import OperacaoForm from "./OperacaoForm.tsx";
import MonitoramentoModal from "../../components/modals/MonitoramentoModal"; // Importar Modal

const OperacoesPage: React.FC = () => {
    const [monitorModalOpen, setMonitorModalOpen] = useState(false);
    const [selectedOperacao, setSelectedOperacao] = useState<Operacao | null>(null);

    const handleOpenMonitor = (operacao: Operacao) => {
        setSelectedOperacao(operacao);
        setMonitorModalOpen(true);
    };

    const handleCloseMonitor = () => {
        setMonitorModalOpen(false);
        setSelectedOperacao(null);
    };

    return (
        <>
            <GenericCrudPage<Operacao>
                title="Operações"
                description="Visualize e gerencie todas as suas operações."
                fetcher={getOperacoes}
                deleter={deleteOperacao}
                columnsFactory={(onRefresh) => getOperacaoColumns(onRefresh, handleOpenMonitor)}
                FormComponent={OperacaoForm}
            />

            {selectedOperacao && (
                <MonitoramentoModal
                    open={monitorModalOpen}
                    onClose={handleCloseMonitor}
                    operacaoId={selectedOperacao.id}
                />
            )}
        </>
    );
};

export default OperacoesPage;