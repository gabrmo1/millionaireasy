import React from 'react';
import FormFieldRenderer from '../../../components/common/forms/FormFieldRenderer';
import { estrategiaFormConfig } from '../estrategiaConfig';

interface Step1Props {
    formData: any;
    handleMainChange: (name: string, value: any) => void;
    errors: Record<string, string | null>;
}

const Step1_InfoGerais: React.FC<Step1Props> = ({ formData, handleMainChange, errors }) => {
    const field = estrategiaFormConfig.find(f => f.name === 'nome')!;

    return (
        <FormFieldRenderer
            field={field}
            formData={formData}
            onChange={handleMainChange}
            error={!!errors.nome}
            helperText={errors.nome}
        />
    );
};

export default Step1_InfoGerais;