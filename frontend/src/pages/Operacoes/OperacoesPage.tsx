import React from 'react';
import GenericCrudPage from '../../components/common/pages/GenericCrudPage';
import { getOperacoes, deleteOperacao } from '../../services/operacaoService';
import { getOperacaoColumns } from './operacaoConfig.tsx';
import type {Operacao} from '../../types/operacao';
import OperacaoForm from "./OperacaoForm.tsx";

const OperacoesPage: React.FC = () => (
    <GenericCrudPage<Operacao>
        title="Operações"
        description="Visualize e gerencie todas as suas operações. Cada linha representa uma instância do bot em execução com uma estratégia definida."
        fetcher={getOperacoes}
        deleter={deleteOperacao}
        columnsFactory={getOperacaoColumns} // Usando a factory
        FormComponent={OperacaoForm}
    />
);

export default OperacoesPage;