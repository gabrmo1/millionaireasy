import React, { useState, useEffect, useMemo } from 'react';
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
    IconButton,
    FormControl,
    InputLabel,
    Select,
    MenuItem,
    FormHelperText,
    FormControlLabel,
    Checkbox,
    StepButton
} from '@mui/material';
import { Visibility, VisibilityOff } from '@mui/icons-material';
import Grid from '@mui/material/GridLegacy';
import type { FormMetadata, FormFieldMetadata, TemplateMetadata } from '../../../types/formMetadata';
import type { BaseEntity } from '../../../types/operador';
import { formatLeadingZeros } from "../../../utils/inputFormatters.ts";

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
}

const EMPTY_DATA = {};

const DynamicForm = <T extends BaseEntity>({ metadata, entityId, fetcher, onSubmit, onUpdate, onClose, templates = {}, customValidator, initialData = EMPTY_DATA }: DynamicFormProps<T>) => {
    const isEditMode = !!entityId;
    const [activeStep, setActiveStep] = useState(0);
    const [formData, setFormData] = useState<Record<string, any>>(initialData);
    const [errors, setErrors] = useState<Record<string, string | null>>({});
    const [loading, setLoading] = useState<boolean>(false);
    const [snackbar, setSnackbar] = useState<{ open: boolean; message: string; severity: 'success' | 'error' }>({ open: false, message: '', severity: 'error' });
    const [showPassword, setShowPassword] = useState<Record<string, boolean>>({});
    const [stepErrors, setStepErrors] = useState<boolean[]>(new Array(metadata.steps.length).fill(false));

    const fieldToStepMap = useMemo(() => {
        const map = new Map<string, number>();
        metadata.steps.forEach((step, index) => {
            step.rows.forEach(row => {
                row.formFields?.forEach(field => {
                    map.set(field.field, index);
                });
            });
        });
        return map;
    }, [metadata]);

    useEffect(() => {
        if (isEditMode && fetcher && entityId) {
            setLoading(true);
            fetcher(entityId)
                .then(data => {
                    const initialData: Record<string, any> = { ...data };
                    metadata.steps.forEach(step => {
                        step.rows.forEach(row => {
                            row.formFields?.forEach(field => {
                                if ((field.type === 'double' || field.type === 'integer') && initialData[field.field] !== undefined && initialData[field.field] !== null) {
                                    initialData[field.field] = String(initialData[field.field]);
                                }
                            });
                        });
                    });
                    setFormData(initialData);
                })
                .catch(err => {
                    console.error("Falha ao carregar dados para edição:", err);
                    setSnackbar({ open: true, message: 'Falha ao carregar dados existentes.', severity: 'error' });
                })
                .finally(() => setLoading(false));
        } else {
            setFormData(initialData);
        }
    }, [entityId, isEditMode, fetcher, metadata, initialData]);

    const handleChange = (field: string, value: any) => {
        setFormData(prev => ({ ...prev, [field]: value }));
        if (errors[field]) {
            setErrors(prev => ({ ...prev, [field]: null }));
        }
    };

    const handleNumericChange = (field: FormFieldMetadata, value: string) => {
        const formattedValue = formatLeadingZeros(value.replace(/[^0-9.]/g, ''));
        handleChange(field.field, formattedValue);
    };

    const handleTogglePasswordVisibility = (field: string) => {
        setShowPassword(prev => ({ ...prev, [field]: !prev[field] }));
    };

    const validateFormFields = (): { errors: Record<string, string | null>, stepErrors: boolean[] } => {
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
                    if (hasError) {
                        stepHasError = true;
                    }
                });
            });
            if (stepHasError) {
                newStepErrors[index] = true;
            }
        });
        return { errors: newTotalErrors, stepErrors: newStepErrors };
    };


    const handleNext = () => {
        setActiveStep(prev => prev + 1);
    };

    const handleBack = () => setActiveStep(prev => prev - 1);

    const handleStep = (step: number) => () => {
        setActiveStep(step);
    };

    const handleSubmit = async () => {
        const fieldValidation = validateFormFields();
        let customErrors: Record<string, string | null> = {};

        if (customValidator) {
            customErrors = customValidator(formData);
            if (Object.values(customErrors).some(e => e !== null)) {
                if (customErrors.indicadores) {
                    const indicatorsStepIndex = metadata.steps.findIndex(s => s.title === 'Indicadores');
                    if (indicatorsStepIndex !== -1) fieldValidation.stepErrors[indicatorsStepIndex] = true;
                }
                Object.keys(customErrors).forEach(fieldKey => {
                    if (fieldToStepMap.has(fieldKey)) {
                        const stepIndex = fieldToStepMap.get(fieldKey)!;
                        fieldValidation.stepErrors[stepIndex] = true;
                    }
                });
            }
        }

        const allErrors = { ...fieldValidation.errors, ...customErrors };

        if (Object.values(allErrors).some(e => e !== null)) {
            setErrors(allErrors);
            setStepErrors(fieldValidation.stepErrors);
            setSnackbar({ open: true, message: 'Existem erros no formulário. Verifique as etapas marcadas em vermelho.', severity: 'error' });
            return;
        }

        setErrors({});
        setStepErrors(new Array(metadata.steps.length).fill(false));
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

    const processDynamicText = (text: string): string => {
        return text.replace(/\${(.*?)}/g, (_, key) => formData[key] || '');
    };

    const renderField = (field: FormFieldMetadata) => {
        if (field.hidden) {
            return null;
        }

        const commonProps = {
            key: field.field,
            label: field.label,
            name: field.field,
            required: !field.nullable,
            error: !!errors[field.field],
        };

        const adornment = field.inputAdornment ? (
            <InputAdornment position={field.inputAdornment.position}>
                {processDynamicText(field.inputAdornment.text)}
            </InputAdornment>
        ) : null;


        switch (field.type) {
            case 'password':
                return (
                    <TextField
                        {...commonProps}
                        type={showPassword[field.field] ? 'text' : 'password'}
                        value={formData[field.field] ?? ''}
                        onChange={(e) => handleChange(field.field, e.target.value)}
                        fullWidth
                        size="small"
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
            case 'double':
            case 'integer':
                return (
                    <TextField
                        {...commonProps}
                        type="number"
                        value={formData[field.field] ?? ''}
                        onChange={(e) => handleNumericChange(field, e.target.value)}
                        fullWidth
                        size="small"
                        helperText={errors[field.field] || processDynamicText(field.description || ' ')}
                        inputProps={{ min: field.minValue, max: field.maxValue }}
                        InputProps={{
                            [field.inputAdornment?.position === 'start' ? 'startAdornment' : 'endAdornment']: adornment
                        }}
                    />
                )
            case 'enum':
                return (
                    <FormControl fullWidth size="small" {...commonProps}>
                        <InputLabel>{field.label}</InputLabel>
                        <Select
                            label={field.label}
                            value={formData[field.field] ?? ''}
                            onChange={(e) => handleChange(field.field, e.target.value)}
                        >
                            {field.options?.map(opt => <MenuItem key={opt} value={opt}>{opt}</MenuItem>)}
                        </Select>
                        <FormHelperText>{errors[field.field] || field.description || ' '}</FormHelperText>
                    </FormControl>
                )
            case 'boolean':
                return (
                    <FormControlLabel
                        control={
                            <Checkbox
                                checked={!!formData[field.field]}
                                onChange={(e) => handleChange(field.field, e.target.checked)}
                                name={field.field}
                                size="small"
                            />
                        }
                        label={field.label}
                    />
                )
            case 'string':
            default:
                return (
                    <TextField
                        {...commonProps}
                        type="text"
                        value={formData[field.field] ?? ''}
                        onChange={(e) => handleChange(field.field, e.target.value)}
                        fullWidth
                        size="small"
                        helperText={errors[field.field] || field.description || ' '}
                    />
                );
        }
    };

    const renderTemplate = (template: TemplateMetadata) => {
        const TemplateComponent = templates[template.name];
        if (!TemplateComponent) {
            return <Alert severity="error">Template "{template.name}" não encontrado.</Alert>;
        }
        return <TemplateComponent formData={formData} errors={errors} handleChange={handleChange} />;
    };

    const currentStep = metadata.steps[activeStep];
    const stepsTitles = metadata.steps.map(s => s.title);

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
            <Stepper nonLinear activeStep={activeStep} sx={{ px: 2, pt: 2, mb: 2 }}>
                {stepsTitles.map((label, index) => (
                    <Step key={label}>
                        <StepButton color="inherit" onClick={handleStep(index)}>
                            <StepLabel error={stepErrors[index]}>{label}</StepLabel>
                        </StepButton>
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
                                    {row.templates?.map(template => (
                                        <Grid item xs={12} sm={template.width} key={template.sequence}>
                                            {renderTemplate(template)}
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