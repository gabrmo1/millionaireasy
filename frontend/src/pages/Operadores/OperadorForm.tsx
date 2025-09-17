import React, { useCallback } from 'react';
import GenericFormPage from '../../components/common/forms/GenericFormPage';
import { operadorFormConfig } from './operadorConfig';
import { createOperador, updateOperador, getOperadorById } from '../../services/operadorService';
import type {CriarOperadorDTO, Operador} from '../../types/operador';

const OperadorForm: React.FC = () => {
    const handleSubmit = useCallback(async (formData: Record<string, any>) => {
        const operadorData = formData as CriarOperadorDTO;
        await createOperador(operadorData);
    }, []);

    const handleUpdate = useCallback(async (id: string, formData: Record<string, any>) => {
        const operadorData = formData as CriarOperadorDTO;
        await updateOperador(id, operadorData);
    }, []);

    return (
        <GenericFormPage<Operador>
            title="Operadores - Criar"
            description="O Operador representa suas credenciais na corretora. As chaves de API (Access Key e Secret Key) são necessárias para que o sistema possa realizar operações em seu nome."
            formConfig={operadorFormConfig}
            onSubmit={handleSubmit}
            onUpdate={handleUpdate}
            fetcher={getOperadorById}
            backRoute="/operadores"
        />
    );
};

export default OperadorForm;