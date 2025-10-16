import { useState, useMemo } from 'react';
import type { FormMetadata } from '../../../../types/formMetadata';

export const useFormValidation = (
    metadata: FormMetadata,
    formData: Record<string, any>,
    customValidator?: (formData: Record<string, any>) => Record<string, string | null>
) => {
    const [errors, setErrors] = useState<Record<string, string | null>>({});
    const [stepErrors, setStepErrors] = useState<boolean[]>(new Array(metadata.steps.length).fill(false));

    const fieldToStepMap = useMemo(() => {
        const map = new Map<string, number>();
        metadata.steps.forEach((step, index) => {
            step.rows.forEach(row => {
                row.formFields?.forEach(field => map.set(field.field, index));
            });
        });
        return map;
    }, [metadata]);

    const validate = (): { isValid: boolean, allErrors: Record<string, string | null> } => {
        const newTotalErrors: Record<string, string | null> = {};
        const newStepErrors = new Array(metadata.steps.length).fill(false);

        metadata.steps.forEach((step, index) => {
            let stepHasError = false;
            step.rows.forEach(row => {
                row.formFields?.forEach(field => {
                    const value = formData[field.field];
                    let hasError = false;
                    if (!field.nullable && (value === undefined || value === null || value === '')) {
                        newTotalErrors[field.field] = `${field.label} é obrigatório.`;
                        hasError = true;
                    }
                    if (field.maxLength && String(value || '').length > field.maxLength) {
                        newTotalErrors[field.field] = `O campo deve ter no máximo ${field.maxLength} caracteres.`;
                        hasError = true;
                    }
                    if (field.minValue !== undefined && value !== '' && Number(value) < field.minValue) {
                        newTotalErrors[field.field] = `O valor deve ser no mínimo ${field.minValue}.`;
                        hasError = true;
                    }
                    if (field.maxValue !== undefined && value !== '' && Number(value) > field.maxValue) {
                        newTotalErrors[field.field] = `O valor deve ser no máximo ${field.maxValue}.`;
                        hasError = true;
                    }
                    if (hasError) stepHasError = true;
                });
            });
            if (stepHasError) newStepErrors[index] = true;
        });

        const customErrors = customValidator ? customValidator(formData) : {};
        Object.assign(newTotalErrors, customErrors);

        Object.keys(customErrors).forEach(fieldKey => {
            if (fieldToStepMap.has(fieldKey)) {
                const stepIndex = fieldToStepMap.get(fieldKey)!;
                newStepErrors[stepIndex] = true;
            } else {
                const stepTitleMap: { [key: string]: string } = {
                    'indicador': 'Indicadores',
                    'condicao_compra': 'Regras de compra',
                    'condicao_venda': 'Regras de Venda',
                };
                for (const prefix in stepTitleMap) {
                    if (fieldKey.startsWith(prefix)) {
                        const stepIndex = metadata.steps.findIndex(s => s.title === stepTitleMap[prefix]);
                        if (stepIndex !== -1) newStepErrors[stepIndex] = true;
                        break;
                    }
                }
            }
        });

        setErrors(newTotalErrors);
        setStepErrors(newStepErrors);

        const isValid = Object.values(newTotalErrors).every(e => e === null);
        return { isValid, allErrors: newTotalErrors };
    };

    return { errors, stepErrors, setErrors, validate };
};