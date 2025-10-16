import { useState, useEffect } from 'react';
import type { BaseEntity } from '../../../../types/operador';
import type { FormMetadata } from '../../../../types/formMetadata';

export const useFormStateAndLifecycle = <T extends BaseEntity>(
    entityId: string | null,
    fetcher: ((id: string) => Promise<T>) | undefined,
    metadata: FormMetadata,
    initialData: Record<string, any>,
    dataTransformer?: (data: T) => Record<string, any>
) => {
    const isEditMode = !!entityId;
    const [formData, setFormData] = useState<Record<string, any>>(initialData);
    const [loading, setLoading] = useState<boolean>(false);
    const [snackbar, setSnackbar] = useState<{ open: boolean; message: string; severity: 'success' | 'error' }>({ open: false, message: '', severity: 'error' });

    useEffect(() => {
        if (isEditMode && fetcher && entityId) {
            setLoading(true);
            fetcher(entityId)
                .then(data => {
                    const transformedData = dataTransformer ? dataTransformer(data) : { ...data };
                    const fetchedData: Record<string, any> = transformedData;

                    metadata.steps.forEach(step => {
                        step.rows.forEach(row => {
                            row.formFields?.forEach(field => {
                                if ((field.type === 'double' || field.type === 'integer') && fetchedData[field.field] != null) {
                                    fetchedData[field.field] = String(fetchedData[field.field]);
                                }
                            });
                        });
                    });
                    setFormData(fetchedData);
                })
                .catch(err => {
                    console.error("Falha ao carregar dados para edição:", err);
                    setSnackbar({ open: true, message: 'Falha ao carregar dados existentes.', severity: 'error' });
                })
                .finally(() => setLoading(false));
        } else {
            setFormData(initialData);
        }
    }, [entityId, isEditMode, fetcher, metadata, initialData, dataTransformer]);

    return {
        formData,
        setFormData,
        loading,
        setLoading,
        snackbar,
        setSnackbar,
    };
};