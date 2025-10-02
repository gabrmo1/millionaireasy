import { useState, useEffect } from 'react';
import {
    Box,
    Stepper,
    Step,
    StepLabel,
    Button,
    CircularProgress,
    Snackbar,
    Alert,
    Typography,
    TextField,
    Divider,
    InputAdornment,
    IconButton
} from '@mui/material';
import { Visibility, VisibilityOff } from '@mui/icons-material';
import Grid from '@mui/material/GridLegacy';
import type { FormMetadata, FormFieldMetadata } from '../../../types/formMetadata';
import type { BaseEntity } from '../../../types/operador';

interface DynamicFormProps<T extends BaseEntity> {
    metadata: FormMetadata;
    entityId: string | null;
    fetcher?: (id: string) => Promise<T>;
    onSubmit: (formData: Record<string, any>) => Promise<void>;
    onUpdate?: (id: string, formData: Record<string, any>) => Promise<void>;
    onClose: () => void;
}

const DynamicForm = <T extends BaseEntity>({ metadata, entityId, fetcher, onSubmit, onUpdate, onClose }: DynamicFormProps<T>) => {
    const isEditMode = !!entityId;
    const [activeStep, setActiveStep] = useState(0);
    const [formData, setFormData] = useState<Record<string, any>>({});
    const [errors, setErrors] = useState<Record<string, string | null>>({});
    const [loading, setLoading] = useState<boolean>(false);
    const [snackbar, setSnackbar] = useState<{ open: boolean; message: string; severity: 'success' | 'error' }>({ open: false, message: '', severity: 'error' });
    const [showPassword, setShowPassword] = useState<Record<string, boolean>>({});

    useEffect(() => {
        if (isEditMode && fetcher && entityId) {
            setLoading(true);
            fetcher(entityId)
                .then(data => {
                    setFormData(data);
                })
                .catch(err => {
                    console.error("Falha ao carregar dados para edição:", err);
                    setSnackbar({ open: true, message: 'Falha ao carregar dados existentes.', severity: 'error' });
                })
                .finally(() => setLoading(false));
        }
    }, [entityId, isEditMode, fetcher]);

    const handleChange = (field: string, value: any) => {
        setFormData(prev => ({ ...prev, [field]: value }));
        if (errors[field]) {
            setErrors(prev => ({ ...prev, [field]: null }));
        }
    };

    const handleTogglePasswordVisibility = (field: string) => {
        setShowPassword(prev => ({ ...prev, [field]: !prev[field] }));
    };

    const validateStep = (stepIndex: number): boolean => {
        const step = metadata.steps[stepIndex];
        const newErrors: Record<string, string | null> = {};
        let isValid = true;

        step.rows.forEach(row => {
            row.formFields?.forEach(field => {
                const value = formData[field.field];
                if (!field.nullable && (value === undefined || value === null || value === '')) {
                    newErrors[field.field] = `${field.label} é obrigatório.`;
                    isValid = false;
                }
                if (field.maxLength && String(value || '').length > field.maxLength) {
                    newErrors[field.field] = `O campo deve ter no máximo ${field.maxLength} caracteres.`;
                    isValid = false;
                }
            });
        });

        setErrors(prev => ({ ...prev, ...newErrors }));
        return isValid;
    };


    const handleNext = () => {
        if (validateStep(activeStep)) {
            setActiveStep(prev => prev + 1);
        }
    };

    const handleBack = () => setActiveStep(prev => prev - 1);

    const handleSubmit = async () => {
        if (!validateStep(activeStep)) {
            setSnackbar({ open: true, message: 'Por favor, corrija os erros no formulário.', severity: 'error' });
            return;
        }

        setLoading(true);
        try {
            if (isEditMode && onUpdate && entityId) {
                await onUpdate(entityId, formData);
            } else {
                await onSubmit(formData);
            }
        } catch (error: any) {
            const message = error.response?.data?.message || 'Ocorreu um erro inesperado.';
            setSnackbar({ open: true, message, severity: 'error' });
        } finally {
            setLoading(false);
        }
    };

    const renderField = (field: FormFieldMetadata) => {
        switch (field.type) {
            case 'password':
                return (
                    <TextField
                        key={field.field}
                        label={field.label}
                        name={field.field}
                        type={showPassword[field.field] ? 'text' : 'password'}
                        value={formData[field.field] ?? ''}
                        onChange={(e) => handleChange(field.field, e.target.value)}
                        required={!field.nullable}
                        fullWidth
                        size="small"
                        error={!!errors[field.field]}
                        helperText={errors[field.field] || field.description || ' '}
                        InputProps={{
                            endAdornment: (
                                <InputAdornment position="end">
                                    <IconButton
                                        aria-label="toggle password visibility"
                                        onClick={() => handleTogglePasswordVisibility(field.field)}
                                        onMouseDown={(e) => e.preventDefault()}
                                        edge="end"
                                    >
                                        {showPassword[field.field] ? <VisibilityOff /> : <Visibility />}
                                    </IconButton>
                                </InputAdornment>
                            ),
                        }}
                    />
                );
            case 'string':
            default:
                return (
                    <TextField
                        key={field.field}
                        label={field.label}
                        name={field.field}
                        type={field.type}
                        value={formData[field.field] ?? ''}
                        onChange={(e) => handleChange(field.field, e.target.value)}
                        required={!field.nullable}
                        fullWidth
                        size="small"
                        error={!!errors[field.field]}
                        helperText={errors[field.field] || field.description || ' '}
                    />
                );
        }
    };

    const currentStep = metadata.steps[activeStep];
    const stepsTitles = metadata.steps.map(s => s.title);

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
            <Stepper activeStep={activeStep} sx={{ px: 2, pt: 2, mb: 2 }}>
                {stepsTitles.map((label) => (
                    <Step key={label}>
                        <StepLabel>{label}</StepLabel>
                    </Step>
                ))}
            </Stepper>

            <Box sx={{ flexGrow: 1, overflow: 'auto', px: 7, pt: 1, pb: 3, position: 'relative' }}>
                {loading ? <CircularProgress sx={{ position: 'absolute', top: '50%', left: '50%' }} /> : (
                    <>
                        {currentStep.rows.map(row => (
                            <Box key={row.number} sx={{ mt: row.subtitle && row.number > 1 ? 4 : 1.5 }}>
                                {row.subtitle && (
                                    <Divider sx={{ mb: 3.5 }}>
                                        <Typography variant="subtitle1" sx={{ fontWeight: 500 }}>
                                            {row.subtitle}
                                        </Typography>
                                    </Divider>
                                )}
                                <Grid container spacing={2.5}>
                                    {row.formFields?.map(field => (
                                        <Grid item xs={12} sm={Number(field.fieldSize)} key={field.sequence}>
                                            {renderField(field)}
                                        </Grid>
                                    ))}
                                </Grid>
                            </Box>
                        ))}
                    </>
                )}
            </Box>

            <Box sx={{ p: 2, borderTop: 1, borderColor: 'divider' }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Button variant="outlined" onClick={onClose} disabled={loading}>Cancelar</Button>
                    <Box>
                        {metadata.steps.length > 1 && (
                            <Button color="inherit" disabled={activeStep === 0 || loading} onClick={handleBack} sx={{ mr: 1 }}>
                                Voltar
                            </Button>
                        )}
                        <Button variant="contained" onClick={activeStep === metadata.steps.length - 1 ? handleSubmit : handleNext} disabled={loading}>
                            {activeStep === metadata.steps.length - 1 ? 'Salvar' : 'Avançar'}
                        </Button>
                    </Box>
                </Box>
            </Box>

            <Snackbar open={snackbar.open} autoHideDuration={6000} onClose={() => setSnackbar(prev => ({ ...prev, open: false }))} anchorOrigin={{ vertical: 'top', horizontal: 'center' }}>
                <Alert onClose={() => setSnackbar(prev => ({ ...prev, open: false }))} severity={snackbar.severity} variant="filled" sx={{ width: '100%' }}>
                    {snackbar.message}
                </Alert>
            </Snackbar>
        </Box>
    );
};

export default DynamicForm;