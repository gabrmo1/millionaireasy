import React from 'react';
import GenericCrudPage from '../../components/common/pages/GenericCrudPage';
import { getOperacoes, deleteOperacao } from '../../services/operacaoService';
import { operacaoGridColumns } from './operacaoConfig';
import type {Operacao} from '../../types/operacao';

const OperacoesPage: React.FC = () => (
    <GenericCrudPage<Operacao>
        title="Operações"
        description="Visualize e gerencie todas as suas operações. Cada linha representa uma instância do bot em execução com uma estratégia definida."
        fetcher={getOperacoes}
        deleter={deleteOperacao}
        gridColumns={operacaoGridColumns}
        createRoute="/operacoes/novo"
        editRoute="operacoes/editar"
    />
);

export default OperacoesPage;