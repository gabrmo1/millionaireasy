import React, { useCallback } from 'react';
import { createOperador, updateOperador, getOperadorById } from '../../services/operadorService';
import type { CriarOperadorDTO, Operador } from '../../types/operador';
import DynamicForm from '../../components/common/forms/DynamicForm';
import { operadorFormMetadata } from './operadorFormMetadata';

interface OperadorFormProps {
    entityId: string | null;
    onClose: () => void;
    onSave: () => void;
}

const OperadorForm: React.FC<OperadorFormProps> = ({ entityId, onClose, onSave }) => {

    const handleSubmit = useCallback(async (formData: Record<string, any>) => {
        const operadorData = formData as CriarOperadorDTO;
        await createOperador(operadorData);
        onSave();
    }, [onSave]);

    const handleUpdate = useCallback(async (id: string, formData: Record<string, any>) => {
        const operadorData = formData as CriarOperadorDTO;
        await updateOperador(id, operadorData);
        onSave();
    }, [onSave]);

    return (
        <DynamicForm<Operador>
            metadata={operadorFormMetadata}
            entityId={entityId}
            fetcher={getOperadorById}
            onSubmit={handleSubmit}
            onUpdate={handleUpdate}
            onClose={onClose}
        />
    );
};

export default OperadorForm;