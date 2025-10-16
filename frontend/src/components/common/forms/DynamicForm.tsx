import React, { useState } from 'react';
import {
    Box,
    Stepper,
    Step,
    StepLabel,
    StepButton,
    CircularProgress,
    Snackbar,
    Alert,
} from '@mui/material';
import type { FormMetadata } from '../../../types/formMetadata';
import type { BaseEntity } from '../../../types/operador';
import { useFormStateAndLifecycle } from './hooks/useFormStateAndLifecycle';
import { useFormValidation } from './hooks/useFormValidation';
import FormStepContent from './FormStepContent';
import FormActions from './FormActions';

export interface TemplateProps {
    formData: Record<string, any>;
    errors: Record<string, string | null>;
    handleChange: (field: string, value: any) => void;
}

interface DynamicFormProps<T extends BaseEntity> {
    metadata: FormMetadata;
    entityId: string | null;
    fetcher?: (id: string) => Promise<T>;
    onSubmit: (formData: Record<string, any>) => Promise<void>;
    onUpdate?: (id: string, formData: Record<string, any>) => Promise<void>;
    onClose: () => void;
    templates?: Record<string, React.ComponentType<TemplateProps>>;
    customValidator?: (formData: Record<string, any>) => Record<string, string | null>;
    initialData?: Record<string, any>;
    dataTransformer?: (data: T) => Record<string, any>;
}

const EMPTY_DATA = {};

const DynamicForm = <T extends BaseEntity>({
                                               metadata,
                                               entityId,
                                               fetcher,
                                               onSubmit,
                                               onUpdate,
                                               onClose,
                                               templates = {},
                                               customValidator,
                                               initialData = EMPTY_DATA,
                                               dataTransformer
                                           }: DynamicFormProps<T>) => {
    const isEditMode = !!entityId;
    const [activeStep, setActiveStep] = useState(0);

    const { formData, setFormData, loading, setLoading, snackbar, setSnackbar } = useFormStateAndLifecycle<T>(
        entityId,
        fetcher,
        metadata,
        initialData,
        dataTransformer
    );

    const { errors, stepErrors, setErrors, validate } = useFormValidation(metadata, formData, customValidator);

    const handleChange = (field: string, value: any) => {
        setFormData(prev => ({ ...prev, [field]: value }));
        if (errors[field]) {
            setErrors(prev => ({ ...prev, [field]: null }));
        }
    };

    const handleFormSubmit = async () => {
        const { isValid } = validate();
        if (!isValid) {
            setSnackbar({ open: true, message: 'Existem erros no formulário. Verifique as etapas marcadas em vermelho.', severity: 'error' });
            return;
        }

        setLoading(true);
        try {
            const submissionData = { ...formData };
            if (isEditMode && onUpdate && entityId) {
                await onUpdate(entityId, submissionData);
            } else {
                await onSubmit(submissionData);
            }
        } catch (error: any) {
            const message = error.response?.data?.message || 'Ocorreu um erro inesperado.';
            setSnackbar({ open: true, message, severity: 'error' });
        } finally {
            setLoading(false);
        }
    };

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
            <Stepper nonLinear activeStep={activeStep} sx={{ px: 2, pt: 2, mb: 2 }}>
                {metadata.steps.map((step, index) => (
                    <Step key={step.title}>
                        <StepButton color="inherit" onClick={() => setActiveStep(index)}>
                            <StepLabel error={stepErrors[index]}>{step.title}</StepLabel>
                        </StepButton>
                    </Step>
                ))}
            </Stepper>

            <Box sx={{ flexGrow: 1, overflow: 'auto', px: 7, pt: 1, pb: 3, position: 'relative' }}>
                {loading ? <CircularProgress sx={{ position: 'absolute', top: '50%', left: '50%' }} /> : (
                    <FormStepContent
                        step={metadata.steps[activeStep]}
                        templates={templates}
                        formData={formData}
                        errors={errors}
                        handleChange={handleChange}
                    />
                )}
            </Box>

            <FormActions
                activeStep={activeStep}
                totalSteps={metadata.steps.length}
                loading={loading}
                onClose={onClose}
                handleBack={() => setActiveStep(p => p - 1)}
                handleNext={() => setActiveStep(p => p + 1)}
                handleSubmit={handleFormSubmit}
            />

            <Snackbar
                open={snackbar.open}
                autoHideDuration={6000}
                onClose={() => setSnackbar(prev => ({ ...prev, open: false }))}
                anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
            >
                <Alert onClose={() => setSnackbar(prev => ({ ...prev, open: false }))} severity={snackbar.severity} variant="filled" sx={{ width: '100%' }}>
                    {snackbar.message}
                </Alert>
            </Snackbar>
        </Box>
    );
};

export default DynamicForm;