import { useState, useCallback } from 'react';
import type { FormMetadata } from '../../../../types/formMetadata';

export const useFormValidation = (
    metadata: FormMetadata,
    formData: Record<string, any>,
    customValidator?: (data: Record<string, any>) => Record<string, string | null>
) => {
    const [errors, setErrors] = useState<Record<string, string | null>>({});

    const validate = useCallback(() => {
        const newErrors: Record<string, string | null> = {};
        let isValid = true;

        metadata.steps.forEach(step => {
            step.rows.forEach(row => {
                row.formFields?.forEach(field => {
                    if (field.hidden) return;

                    const value = formData[field.field];

                    if (!field.nullable) {
                        if (value === undefined || value === '' || value === null) {
                            newErrors[field.field] = 'Este campo é obrigatório';
                            isValid = false;
                        } else if (Array.isArray(value) && value.length === 0) {
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
            });
        });

        if (customValidator) {
            const customErrors = customValidator(formData);
            Object.keys(customErrors).forEach(key => {
                if (customErrors[key]) {
                    newErrors[key] = customErrors[key];
                    isValid = false;
                }
            });
        }

        setErrors(newErrors);
        return { isValid, errors: newErrors };
    }, [formData, metadata, customValidator]);

    const stepErrors: Record<number, boolean> = {};
    metadata.steps.forEach((step, index) => {
        // Coleta todos os campos deste step
        const stepFields = new Set<string>();
        step.rows.forEach(row =>
            row.formFields?.forEach(field => stepFields.add(field.field))
        );

        const hasError = Object.keys(errors).some(key => stepFields.has(key) && errors[key]);
        if (hasError) {
            stepErrors[index] = true;
        }
    });

    return {
        errors,
        stepErrors,
        setErrors,
        validate
    };
};