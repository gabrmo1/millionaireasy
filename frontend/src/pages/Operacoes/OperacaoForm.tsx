import React, { useCallback } from 'react';
import GenericFormPage from '../../components/common/forms/GenericFormPage';
import { operacaoFormConfig } from './operacaoFormConfig';
import api from '../../services/api';
import type {Operacao} from '../../types/operacao';
import {getOperacaoById, updateOperacao} from "../../services/operacaoService.ts";

const OperacaoForm: React.FC = () => {

    const createOperacao = useCallback(async (data: Record<string, any>) => {
        const payload = {
            ...data,
            dataInicio: new Date().toISOString(),
            dataFim: new Date(new Date().setFullYear(new Date().getFullYear() + 1)).toISOString(),
        };
        await api.post('/v1/operacoes/criar', payload);
    }, []);

    const handleUpdate = useCallback(async (id: string, formData: Record<string, any>) => {
        const payload = {
            ...formData,
            dataInicio: new Date().toISOString(),
            dataFim: new Date(new Date().setFullYear(new Date().getFullYear() + 1)).toISOString(),
        };
        await updateOperacao(id, payload);
    }, []);

    return (
        <GenericFormPage<Operacao>
            title="Operações - Criar"
            description="Uma Operação é a execução do bot. Ela une um Par de Moedas, um Operador (suas chaves) e as Configurações de Análise, Compra e Venda para automatizar sua estratégia."
            formConfig={operacaoFormConfig}
            onSubmit={createOperacao}
            onUpdate={handleUpdate}
            fetcher={getOperacaoById}
            backRoute="/operacoes"
        />
    );
};

export default OperacaoForm;