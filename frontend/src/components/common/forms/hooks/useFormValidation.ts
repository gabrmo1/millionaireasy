import { useState, useCallback } from 'react';
import type { FormFieldMetadata, FormStepMetadata } from '../../../../types/formMetadata';

interface UseFormValidationProps {
    formData: Record<string, any>;
    steps: FormStepMetadata[];
    currentStep: number;
}

export const useFormValidation = ({ formData, steps, currentStep }: UseFormValidationProps) => {
    const [errors, setErrors] = useState<Record<string, string | null>>({});

    const validateStep = useCallback((): boolean => {
        const newErrors: Record<string, string | null> = {};
        let isValid = true;
        const currentFields: FormFieldMetadata[] = [];

        steps[currentStep].rows.forEach(row => {
            if (row.formFields) {
                currentFields.push(...row.formFields);
            }
        });

        currentFields.forEach(field => {
            if (field.hidden) return;
            const value = formData[field.field];

            if (!field.nullable) {
                if (value === undefined || value === '' || value === null) {
                    newErrors[field.field] = 'Este campo é obrigatório';
                    isValid = false;
                }
            }

            if (field.validateMatches) {
                const valueToMatch = formData[field.validateMatches];

                if (value && valueToMatch && value !== valueToMatch) {
                    newErrors[field.field] = 'Os valores não coincidem';
                    isValid = false;
                }
            }
        });

        setErrors(newErrors);
        return isValid;
    }, [formData, steps, currentStep]);

    const clearErrors = useCallback(() => {
        setErrors({});
    }, []);

    return {
        errors,
        validateStep,
        clearErrors
    };
};