import React, { useCallback } from 'react';
import GenericFormPage from '../../components/common/forms/GenericFormPage';
import { operacaoFormConfig } from './operacaoFormConfig';
import { getOperacaoById, updateOperacao } from "../../services/operacaoService.ts";
import { createOperacao as createOperacaoService } from "../../services/operacaoService.ts"; // Supondo que você crie esta função no service
import type { Operacao, CriarOperacaoDTO } from '../../types/operacao';

const OperacaoForm: React.FC = () => {

    // Função para criar a operação
    const handleCreate = useCallback(async (formData: Record<string, any>) => {
        const payload: CriarOperacaoDTO = {
            par: formData.par,
            intervalo: formData.intervalo,
            idOperador: formData.idOperador,
            idEstrategia: formData.idEstrategia,
            dataInicio: new Date().toISOString(),
            dataFim: new Date(new Date().setFullYear(new Date().getFullYear() + 1)).toISOString(),
        };
        // Idealmente, a chamada da API também estaria em 'operacaoService.ts'
        await createOperacaoService(payload);
    }, []);

    // Função para atualizar a operação
    const handleUpdate = useCallback(async (id: string, formData: Record<string, any>) => {
        const payload: CriarOperacaoDTO = {
            par: formData.par,
            intervalo: formData.intervalo,
            idOperador: formData.idOperador,
            idEstrategia: formData.idEstrategia,
            dataInicio: formData.dataInicio || new Date().toISOString(),
            dataFim: formData.dataFim || new Date(new Date().setFullYear(new Date().getFullYear() + 1)).toISOString(),
        };
        await updateOperacao(id, payload);
    }, []);

    return (
        <GenericFormPage<Operacao>
            title="Operações - Criar"
            description="Uma Operação é a execução do bot. Ela une um Par de Moedas, um Operador (suas chaves) e uma Estratégia para automatizar o trading."
            formConfig={operacaoFormConfig}
            onSubmit={handleCreate}
            onUpdate={handleUpdate}
            fetcher={getOperacaoById}
            backRoute="/operacoes"
        />
    );
};

export default OperacaoForm;