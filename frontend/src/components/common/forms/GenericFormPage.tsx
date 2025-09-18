import React, { useState, useEffect } from 'react';
import { alpha } from '@mui/material/styles'
import { useNavigate, useParams } from 'react-router-dom';
import {
    Paper, Typography, Box, Button, TextField, Checkbox,
    FormControlLabel, FormControl, InputLabel, Select, MenuItem, FormHelperText,
    Snackbar, Alert, Collapse, CircularProgress
} from '@mui/material';
import Grid from '@mui/material/GridLegacy';
import type {FormField, BaseEntity} from '../../../types/common';
import EntitySelectorField from './EntitySelectorField';

interface GenericFormPageProps<T extends BaseEntity> {
    title: string;
    description?: string;
    formConfig: FormField<T>[];
    onSubmit: (formData: Record<string, any>) => Promise<void>;
    onUpdate?: (id: string, formData: Record<string, any>) => Promise<void>;
    fetcher?: (id: string) => Promise<T>;
    backRoute: string;
}

export default function GenericFormPage<T extends BaseEntity>({
                                                                  title,
                                                                  description,
                                                                  formConfig,
                                                                  onSubmit,
                                                                  onUpdate,
                                                                  fetcher,
                                                                  backRoute,
                                                              }: GenericFormPageProps<T>) {
    const navigate = useNavigate();
    const { id } = useParams<{ id: string }>();
    const isEditMode = !!id;

    const getInitialState = () => {
        const initialState: Record<string, any> = {};
        formConfig.forEach(field => {
            initialState[field.name] = field.defaultValue ?? '';
            if (field.type === 'checkbox') {
                initialState[field.name] = field.defaultValue ?? false;
            }
        });
        return initialState;
    };

    const [formData, setFormData] = useState<Record<string, any>>(getInitialState());
    const [errors, setErrors] = useState<Record<string, string | null>>({});
    const [animatingErrors, setAnimatingErrors] = useState<Record<string, boolean>>({});
    const [loading, setLoading] = useState<boolean>(false);

    const [snackbarOpen, setSnackbarOpen] = useState(false);
    const [snackbarMessage, setSnackbarMessage] = useState('');

    useEffect(() => {
        if (isEditMode && fetcher) {
            setLoading(true);
            fetcher(id)
                .then((data) => {
                    const fetchedData: Record<string, any> = {};
                    formConfig.forEach(field => {
                        // Lógica para mapear IDs de objetos aninhados (ex: data.operador.id -> formData.idOperador)
                        if (field.type === 'entitySelector' && field.name.startsWith('id')) {
                            const objectName = field.name.substring(2).charAt(0).toLowerCase() + field.name.substring(3);
                            fetchedData[field.name] = data[objectName as keyof T]?.id ?? '';
                        } else {
                            fetchedData[field.name] = data[field.name as keyof T] ?? (field.type === 'checkbox' ? false : '');
                        }
                    });
                    setFormData(fetchedData);
                })
                .catch(err => {
                    console.error(`Falha ao carregar dados para ${title}:`, err);
                    setSnackbarMessage('Falha ao carregar dados para edição.');
                    setSnackbarOpen(true);
                })
                .finally(() => setLoading(false));
        }
    }, [id, isEditMode, fetcher, formConfig, title]);


    const handleChange = (name: string, value: any) => {
        setFormData(prev => ({ ...prev, [name]: value }));
        if (errors[name]) {
            setErrors(prev => ({ ...prev, [name]: null }));
        }
    };

    const handleCheckboxChange = (name: string, checked: boolean) => {
        setFormData(prev => ({ ...prev, [name]: checked }));
    };

    const handleSubmit = async (event: React.FormEvent) => {
        event.preventDefault();

        const newErrors: Record<string, string | null> = {};
        let isValid = true;

        for (const field of formConfig) {
            const isDependentAndHidden = field.dependentOn && !formData[field.dependentOn];
            if (isDependentAndHidden) continue;

            const value = formData[field.name];
            let fieldIsValid = true;

            if (field.required && (value === null || value === undefined || value === '')) {
                newErrors[field.name] = `${field.label} é obrigatório.`;
                isValid = false;
                fieldIsValid = false;
            }

            if (fieldIsValid && field.validation && (value !== null && value !== undefined && value !== '')) {
                const rules = field.validation;

                if (rules.max && Number(value) > rules.max.value) {
                    newErrors[field.name] = rules.max.message;
                    isValid = false;
                }

                if (rules.min && Number(value) < rules.min.value) {
                    newErrors[field.name] = rules.min.message;
                    isValid = false;
                }

                if (rules.pattern && !rules.pattern.value.test(String(value))) {
                    newErrors[field.name] = rules.pattern.message;
                    isValid = false;
                }

                if (rules.maxLength && String(value).length > rules.maxLength.value) {
                    newErrors[field.name] = rules.maxLength.message;
                    isValid = false;
                }
            }
        }

        setErrors(newErrors);

        if (!isValid) {
            const fieldsToAnimate = Object.keys(newErrors).reduce((acc, key) => {
                acc[key] = true;
                return acc;
            }, {} as Record<string, boolean>);

            setAnimatingErrors(fieldsToAnimate);
            setTimeout(() => setAnimatingErrors({}), 1000);

            setSnackbarMessage('Por favor, corrija os erros no formulário antes de continuar.');
            setSnackbarOpen(true);

            return;
        }

        try {
            const payload = { ...formData };
            formConfig.forEach(field => {
                if (field.type === 'select' && payload[field.name] === '') {
                    payload[field.name] = null;
                }
            });

            if (isEditMode && onUpdate && id) {
                await onUpdate(id, payload);
            } else {
                await onSubmit(payload);
            }
            navigate(backRoute);
        } catch (error: any) {
            console.error(`Falha ao submeter o formulário ${title}:`, error);
            setSnackbarMessage(error.response?.data?.message || error.message || 'Ocorreu um erro inesperado.');
            setSnackbarOpen(true);
        }
    };

    const handleSnackbarClose = (_event?: React.SyntheticEvent | Event, reason?: string) => {
        if (reason === 'clickaway') {
            return;
        }
        setSnackbarOpen(false);
    };

    const renderField = (field: FormField<any>) => {
        const hasErrorAnimation = animatingErrors[field.name];
        const hasPersistentError = !!errors[field.name];
        const helperText = errors[field.name];

        switch (field.type) {
            case 'entitySelector':
                return (
                    <div className={hasErrorAnimation ? 'shake-error' : ''}>
                        <EntitySelectorField
                            label={field.label}
                            value={formData[field.name]}
                            modalTitle={field.modalConfig!.title}
                            fetcher={field.modalConfig!.fetcher}
                            displayAttribute={field.modalConfig!.displayAttribute}
                            onChange={(id) => handleChange(field.name, id)}
                            error={hasErrorAnimation}
                            helperText={helperText}
                            helperTextError={hasPersistentError}
                            required={field.required}
                            size="small"
                        />
                    </div>
                );
            case 'checkbox':
                return (
                    <FormControlLabel
                        control={ <Checkbox checked={!!formData[field.name]} onChange={(e) => handleCheckboxChange(field.name, e.target.checked)} name={field.name} size="small" /> }
                        label={field.label}
                    />
                );
            case 'select':
                return (
                    <FormControl fullWidth required={field.required} error={hasErrorAnimation} className={hasErrorAnimation ? 'shake-error' : ''} size="small">
                        <InputLabel>{field.label}</InputLabel>
                        <Select
                            name={field.name}
                            label={field.label}
                            value={formData[field.name] ?? ''}
                            onChange={(e) => handleChange(field.name, e.target.value)}
                        >
                            {field.options?.map(option => ( <MenuItem key={option.value} value={option.value}> {option.label} </MenuItem> ))}
                        </Select>
                        <FormHelperText error={hasPersistentError}>{helperText || ' '}</FormHelperText>
                    </FormControl>
                );
            case 'text':
            case 'number':
            case 'password':
            default:
                return (
                    <TextField
                        label={field.label}
                        name={field.name}
                        type={field.type}
                        value={formData[field.name] ?? ''}
                        onChange={(e) => handleChange(field.name, e.target.value)}
                        required={field.required}
                        fullWidth
                        size="small"
                        error={hasErrorAnimation}
                        helperText={helperText || ' '}
                        className={hasErrorAnimation ? 'shake-error' : ''}
                        FormHelperTextProps={{ style: { color: hasPersistentError ? '#d32f2f' : undefined } }}
                    />
                );
        }
    };

    const independentFields = formConfig.filter(field => !field.dependentOn);

    return (
        <>
            <Paper
                elevation={4}
                sx={{
                    height: 'calc(100vh - 112px)',
                    display: 'flex',
                    flexDirection: 'column',
                    overflow: 'hidden',
                    // Efeito Glassmorphism Ajustado
                    backgroundColor: (theme) => alpha(theme.palette.background.paper, 0.6), // Mais sutil
                    backdropFilter: 'blur(12px)',
                    borderRadius: '16px',
                    border: (theme) => `1px solid ${alpha(theme.palette.text.primary, 0.1)}`,
                }}
            >
                {/* Cabeçalho do Formulário */}
                <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider', bgcolor: 'transparent' }}>
                    <Typography variant="h5" component="h2">
                        {isEditMode ? `${title} - Editar` : title}
                    </Typography>
                </Box>

                {/* Caixa de Descrição (Tip/Note) */}
                <Box sx={{ overflow: 'auto', p: 1, borderBottom: 1, borderColor: 'divider' }}>
                    {description && (
                        <Alert severity="info" sx={{ backgroundColor: 'rgba(2, 136, 209, 0.1)', color: 'info.dark' }}>
                            {description}
                        </Alert>
                    )}
                </Box>

                {/* Corpo do Formulário (com rolagem) */}
                <Box component="form" onSubmit={handleSubmit} noValidate sx={{ display: 'flex', flexDirection: 'column', flexGrow: 1, overflow: 'hidden' }}>
                    <Box sx={{ flexGrow: 1, overflow: 'auto', py: 3, px: 7, position: 'relative' }}>
                        {loading && (
                            <Box sx={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: 'rgba(255, 255, 255, 0.7)', zIndex: 1 }}>
                                <CircularProgress />
                            </Box>
                        )}
                        <Grid container spacing={1.5}>
                            {independentFields.map(field => {
                                const dependentFields = formConfig.filter(
                                    (depField) => depField.dependentOn === field.name
                                );

                                return (
                                    <Grid xs={12} sm={field.gridSpan ?? 12} key={field.name}>
                                        {renderField(field)}
                                        {dependentFields.length > 0 && (
                                            <Collapse in={!!formData[field.name]} timeout="auto" unmountOnExit>
                                                <Box sx={{ pl: 2, pt: 1.5, borderLeft: 2, borderColor: 'divider', ml: 1.5, mt: 1 }}>
                                                    <Grid container spacing={1.5}>
                                                        {dependentFields.map(depField => (
                                                            <Grid xs={12} sm={depField.gridSpan ?? 12} key={depField.name}>
                                                                {renderField(depField)}
                                                            </Grid>
                                                        ))}
                                                    </Grid>
                                                </Box>
                                            </Collapse>
                                        )}
                                    </Grid>
                                );
                            })}
                        </Grid>
                    </Box>

                    {/* Rodapé Fixo com Botões */}
                    <Box sx={{ p: 2, borderTop: 1, borderColor: 'divider', bgcolor: 'transparent' }}>
                        <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
                            <Button type="submit" variant="contained" color="primary">
                                Salvar
                            </Button>
                            <Button variant="outlined" onClick={() => navigate(backRoute)}>
                                Cancelar
                            </Button>
                        </Box>
                    </Box>
                </Box>
            </Paper>

            <Snackbar
                open={snackbarOpen}
                autoHideDuration={6000}
                onClose={handleSnackbarClose}
                anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
                sx={{ mt: '64px' }}
            >
                <Alert
                    onClose={handleSnackbarClose}
                    severity="error"
                    variant="filled"
                    sx={{ width: '100%' }}
                >
                    {snackbarMessage}
                </Alert>
            </Snackbar>
        </>
    );
}