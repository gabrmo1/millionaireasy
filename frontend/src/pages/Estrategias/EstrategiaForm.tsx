import React, { useState, useEffect } from 'react';
import { alpha } from '@mui/material';
import { useNavigate, useParams } from 'react-router-dom';
import { Paper, Typography, Box, Button, CircularProgress, Alert, Snackbar, Divider, Collapse, TextField, Checkbox, FormControlLabel, Stepper, Step, StepLabel, List, ListItem, ListItemText } from '@mui/material';
import Grid from '@mui/material/GridLegacy';
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import axios from 'axios';

import { getEstrategiaById, createEstrategia, updateEstrategia } from '../../services/estrategiaService';
import type { Estrategia, CondicaoCompraDTO, CondicaoVendaDTO } from '../../types/estrategia';
import { estrategiaFormConfig } from './estrategiaConfig';

import FormFieldRenderer from '../../components/common/forms/FormFieldRenderer';
import CondicaoCompraForm from './CondicaoCompraForm';
import CondicaoVendaForm from './CondicaoVendaForm';
import {tipoIndicadorLabels} from "../../utils/enumMappings.ts";

const steps = ['Informações Gerais', 'Parâmetros de Análise', 'Regras de Compra', 'Regras de Venda', 'Revisão'];

const getInitialState = (): Omit<Estrategia, 'id'> => ({
    nome: '',
    utilizarRsiCurto: false,
    utilizarRsiMedio: false,
    utilizarRsiLongo: false,
    utilizarRsiEstocastico: false,
    utilizarEma: false,
    utilizarSma: false,
    realizarLeituraVolume: false,
    vendaApenasPorLucro: false,
    condicoesCompra: [],
    condicoesVenda: [],
});

const EstrategiaForm: React.FC = () => {
    const navigate = useNavigate();
    const { id } = useParams<{ id: string }>();
    const isEditMode = !!id;

    const [activeStep, setActiveStep] = useState(0);
    const [estrategia, setEstrategia] = useState<Omit<Estrategia, 'id'>>(getInitialState());
    const [loading, setLoading] = useState<boolean>(false);
    const [snackbar, setSnackbar] = useState<{ open: boolean; message: string; severity: 'success' | 'error' }>({ open: false, message: '', severity: 'error' });
    const [errors, setErrors] = useState<Record<string, string | null>>({});

    // Carregamento dos dados para edição
    useEffect(() => {
        if (isEditMode) {
            setLoading(true);
            getEstrategiaById(id)
                .then(data => {
                    const dataWithClientIds = { ...data, condicoesCompra: data.condicoesCompra.map(c => ({...c, clientId: Math.random()})), condicoesVenda: data.condicoesVenda.map(v => ({...v, clientId: Math.random()})), };
                    setEstrategia(dataWithClientIds);
                })
                .catch(err => {
                    console.error("Falha ao carregar estratégia:", err);
                    setSnackbar({ open: true, message: 'Falha ao carregar dados para edição.', severity: 'error' });
                })
                .finally(() => setLoading(false));
        }
    }, [id, isEditMode]);

    // Handlers de Mudança
    const handleMainChange = (name: string, value: string | number | boolean | undefined) => {
        if (errors[name]) {
            setErrors(prev => ({ ...prev, [name]: null }));
        }
        setEstrategia(prev => ({ ...prev, [name]: value }));
    };

    const handleValorOperacaoChange = (name: 'valorOperacaoFixo' | 'percentualValorOperacao', value: string | undefined) => {
        if (errors.valorOperacaoFixo || errors.percentualValorOperacao) {
            setErrors(prev => ({ ...prev, valorOperacaoFixo: null, percentualValorOperacao: null }));
        }
        const numericValue = value ? Number(value) : undefined;
        setEstrategia(prev => {
            const newState = { ...prev, [name]: numericValue };
            if (name === 'valorOperacaoFixo' && numericValue !== undefined) newState.percentualValorOperacao = undefined;
            if (name === 'percentualValorOperacao' && numericValue !== undefined) newState.valorOperacaoFixo = undefined;
            return newState;
        });
    };

    const handlePercentChange = (name: 'percentualValorOperacao' | 'percentualLucro', value: string, max: number) => {
        if (name === 'percentualValorOperacao' && (errors.valorOperacaoFixo || errors.percentualValorOperacao)) {
            setErrors(prev => ({ ...prev, valorOperacaoFixo: null, percentualValorOperacao: null }));
        }
        if (value === '') { handleMainChange(name, undefined); return; }
        const numValue = Number(value);
        let finalValue = numValue;
        if (numValue < 0) finalValue = 0;
        if (numValue > max) finalValue = max;
        if (name === 'percentualValorOperacao') { handleValorOperacaoChange(name, String(finalValue)); }
        else { handleMainChange(name, finalValue); }
    };

    // Handlers de Condições (Compra/Venda)
    const addCondicaoCompra = () => setEstrategia(prev => ({ ...prev, condicoesCompra: [...prev.condicoesCompra, { clientId: Math.random(), tipoIndicador: 'RSI_CURTO' }] }));
    const updateCondicaoCompra = (index: number, updated: CondicaoCompraDTO) => { const newCondicoes = [...estrategia.condicoesCompra]; newCondicoes[index] = updated; setEstrategia(prev => ({ ...prev, condicoesCompra: newCondicoes })); };
    const removeCondicaoCompra = (index: number) => setEstrategia(prev => ({ ...prev, condicoesCompra: prev.condicoesCompra.filter((_, i) => i !== index) }));

    const addCondicaoVenda = () => setEstrategia(prev => ({ ...prev, condicoesVenda: [...prev.condicoesVenda, { clientId: Math.random(), tipoIndicador: 'RSI_CURTO' }] }));
    const updateCondicaoVenda = (index: number, updated: CondicaoVendaDTO) => { const newCondicoes = [...estrategia.condicoesVenda]; newCondicoes[index] = updated; setEstrategia(prev => ({ ...prev, condicoesVenda: newCondicoes })); };
    const removeCondicaoVenda = (index: number) => setEstrategia(prev => ({ ...prev, condicoesVenda: prev.condicoesVenda.filter((_, i) => i !== index) }));

    // Validação e Navegação do Wizard
    const isStepValid = (step: number): boolean => {
        const newErrors: Record<string, string | null> = {};
        let isValid = true;
        switch (step) {
            case 0:
                if (!estrategia.nome.trim()) {
                    newErrors.nome = 'O nome da estratégia é obrigatório.';
                    isValid = false;
                }
                break;
            case 2:
                if (estrategia.condicoesCompra.length > 0) {
                    const valorFixo = estrategia.valorOperacaoFixo;
                    const valorPercentual = estrategia.percentualValorOperacao;
                    if ((!valorFixo || valorFixo <= 0) && (!valorPercentual || valorPercentual <= 0)) {
                        const errorMsg = "Defina um valor de operação.";
                        newErrors.valorOperacaoFixo = errorMsg;
                        newErrors.percentualValorOperacao = errorMsg;
                        setSnackbar({ open: true, message: `É necessário definir um Valor Fixo ou Percentual de operação.`, severity: 'error' });
                        isValid = false;
                    }
                }
                break;
        }
        setErrors(newErrors);
        return isValid;
    };

    const handleNext = () => {
        if (isStepValid(activeStep)) {
            setActiveStep((prevActiveStep) => prevActiveStep + 1);
        }
    };
    const handleBack = () => setActiveStep((prevActiveStep) => prevActiveStep - 1);

    // Submissão do Formulário
    const handleSubmit = async () => {
        if (!isStepValid(activeStep)) return;

        setLoading(true);
        try {
            if (isEditMode) { await updateEstrategia(id, estrategia); }
            else { await createEstrategia(estrategia); }
            navigate('/estrategias');
        } catch (error) {
            console.error("Falha ao salvar estratégia:", error);
            let message = 'Ocorreu um erro inesperado.';
            if (axios.isAxiosError(error) && error.response) { message = error.response.data.message || message; }
            else if (error instanceof Error) { message = error.message; }
            setSnackbar({ open: true, message, severity: 'error' });
        } finally {
            setLoading(false);
        }
    };

    // Renderização do Conteúdo de Cada Etapa
    const getStepContent = (step: number) => {
        switch (step) {
            case 0: {
                const field = estrategiaFormConfig.find(f => f.name === 'nome')!;
                return <FormFieldRenderer
                    field={field}
                    formData={estrategia}
                    onChange={handleMainChange}
                    error={!!errors.nome}
                    helperText={errors.nome}
                />;
            }
            case 1:
                return (
                    <Grid container spacing={1.5}>
                        {estrategiaFormConfig.filter(field => field.type === 'checkbox').map(field => {
                            const dependentFields = estrategiaFormConfig.filter(depField => depField.dependentOn === field.name);
                            return (
                                <Grid item xs={12} sm={12} key={field.name}>
                                    <FormFieldRenderer field={field} formData={estrategia} onChange={handleMainChange} />
                                    {dependentFields.length > 0 && (
                                        <Collapse in={!!estrategia[field.name as keyof typeof estrategia]} timeout="auto" unmountOnExit>
                                            <Box sx={{ pl: 2, pt: 1.5, borderLeft: 2, borderColor: 'divider', ml: 1.5, mt: 1 }}>
                                                <Grid container spacing={1.5}>
                                                    {dependentFields.map(depField => (
                                                        <Grid item xs={12} sm={depField.gridSpan ?? 12} key={depField.name}>
                                                            <FormFieldRenderer field={depField} formData={estrategia} onChange={handleMainChange}/>
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
                );
            case 2:
                return (
                    <>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                            <Typography variant="h6">Condições de Compra</Typography>
                            <Button startIcon={<AddCircleOutlineIcon />} onClick={addCondicaoCompra}>Adicionar</Button>
                        </Box>
                        {estrategia.condicoesCompra.map((condicao, index) => <CondicaoCompraForm key={condicao.clientId} index={index} condicao={condicao} estrategia={estrategia} onUpdate={updateCondicaoCompra} onRemove={removeCondicaoCompra} /> )}
                        {estrategia.condicoesCompra.length === 0 && <Alert severity="info">Nenhuma condição de compra adicionada.</Alert>}
                        <Divider sx={{ my: 3 }} />
                        <Box>
                            <Typography variant="h6" sx={{ mb: 2 }}>Parâmetros de Compra</Typography>
                            <Grid container spacing={2}>
                                <Grid item xs={6}>
                                    <TextField label="Valor Fixo Operação (Ex: 10.50)" type="number" size="small" fullWidth value={estrategia.valorOperacaoFixo ?? ''} onChange={(e) => handleValorOperacaoChange('valorOperacaoFixo', e.target.value)} disabled={!!estrategia.percentualValorOperacao} inputProps={{ min: 0 }} error={!!errors.valorOperacaoFixo} helperText={errors.valorOperacaoFixo || ' '} />
                                </Grid>
                                <Grid item xs={6}>
                                    <TextField label="% do Saldo na Operação" type="number" size="small" fullWidth value={estrategia.percentualValorOperacao ?? ''} onChange={(e) => handlePercentChange('percentualValorOperacao', e.target.value, 100)} disabled={!!estrategia.valorOperacaoFixo} error={!!errors.percentualValorOperacao} helperText={errors.percentualValorOperacao || ' '} />
                                </Grid>
                            </Grid>
                        </Box>
                    </>
                );
            case 3:
                return (
                    <>
                        <Box>
                            <Typography variant="h6" sx={{ mb: 2 }}>Parâmetros de Venda</Typography>
                            <FormControlLabel control={<Checkbox checked={!!estrategia.vendaApenasPorLucro} onChange={(e) => handleMainChange('vendaApenasPorLucro', e.target.checked)} name="vendaApenasPorLucro" size="small" />} label="Efetuar venda somente sobre % de lucro" />
                            <Collapse in={!!estrategia.vendaApenasPorLucro} timeout="auto" unmountOnExit>
                                <Box sx={{ pl: 2, pt: 1.5, ml: 1.5, mt: 1 }}><TextField label="% de Lucro para Venda" type="number" size="small" fullWidth value={estrategia.percentualLucro ?? ''} onChange={(e) => handlePercentChange('percentualLucro', e.target.value, 9999)} sx={{ maxWidth: '300px' }} /></Box>
                            </Collapse>
                        </Box>
                        <Divider sx={{ my: 3 }} />
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                            <Typography variant="h6">Condições de Venda (Opcional)</Typography>
                            <Button startIcon={<AddCircleOutlineIcon />} onClick={addCondicaoVenda}>Adicionar</Button>
                        </Box>
                        {estrategia.condicoesVenda.map((condicao, index) => <CondicaoVendaForm key={condicao.clientId} index={index} condicao={condicao} estrategia={estrategia} onUpdate={updateCondicaoVenda} onRemove={removeCondicaoVenda} />)}
                        {estrategia.condicoesVenda.length === 0 && <Alert severity="info">Nenhuma condição de venda por indicador adicionada.</Alert>}
                    </>
                );
            case 4:
                return (
                    <Box>
                        <Typography variant="h6" gutterBottom>Revise sua Estratégia</Typography>
                        <List dense>
                            <ListItem><ListItemText primary="Nome" secondary={estrategia.nome} /></ListItem>
                            <ListItem><ListItemText primary="Condições de Compra" secondary={estrategia.condicoesCompra.length} /></ListItem>
                            <ListItem><ListItemText primary="Condições de Venda" secondary={estrategia.condicoesVenda.length} /></ListItem>
                            <ListItem><ListItemText primary="Valor da Operação" secondary={estrategia.valorOperacaoFixo ? `$${estrategia.valorOperacaoFixo}` : `${estrategia.percentualValorOperacao}% do saldo`} /></ListItem>
                            <ListItem><ListItemText primary="Venda por Lucro" secondary={estrategia.vendaApenasPorLucro ? `Sim, com ${estrategia.percentualLucro || 0}% de lucro` : 'Não'} /></ListItem>
                            <ListItem><ListItemText primary="Indicadores Ativos" secondary={Object.entries(estrategia).filter(([key, value]) => key.startsWith('utilizar') && value).map(([key]) => tipoIndicadorLabels[key as keyof typeof tipoIndicadorLabels] || key).join(', ')} /></ListItem>
                        </List>
                    </Box>
                );
            default: return 'Passo desconhecido';
        }
    };

    return (
        <>
            <Paper elevation={4} sx={{ height: 'calc(100vh - 112px)', display: 'flex', flexDirection: 'column', overflow: 'hidden', backgroundColor: (theme) => alpha(theme.palette.background.paper, 0.6), backdropFilter: 'blur(12px)', borderRadius: '16px', border: (theme) => `1px solid ${alpha(theme.palette.text.primary, 0.1)}` }}>
                <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
                    <Typography variant="h5" component="h2">{isEditMode ? 'Editar Estratégia' : 'Criar Estratégia'}</Typography>
                </Box>

                <Stepper activeStep={activeStep} sx={{ px: 3, pt: 3 }}>
                    {steps.map((label) => <Step key={label}><StepLabel>{label}</StepLabel></Step>)}
                </Stepper>

                <Box sx={{ flexGrow: 1, overflow: 'auto', p: 4, position: 'relative' }}>
                    {loading ? <CircularProgress sx={{ position: 'absolute', top: '50%', left: '50%' }}/> : getStepContent(activeStep)}
                </Box>

                <Box sx={{ p: 2, borderTop: 1, borderColor: 'divider' }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                        <Box>
                            <Button
                                variant="outlined"
                                onClick={() => navigate('/estrategias')}
                                sx={{ mr: 1 }}
                            >
                                Cancelar
                            </Button>
                        </Box>
                        <Box>
                            <Button
                                color="inherit"
                                disabled={activeStep === 0 || loading}
                                onClick={handleBack}
                                sx={{ mr: 1 }}
                            >
                                Voltar
                            </Button>
                            <Button
                                variant="contained"
                                onClick={activeStep === steps.length - 1 ? handleSubmit : handleNext}
                                disabled={loading}
                            >
                                {activeStep === steps.length - 1 ? 'Salvar Estratégia' : 'Avançar'}
                            </Button>
                        </Box>
                    </Box>
                </Box>
            </Paper>

            <Snackbar open={snackbar.open} autoHideDuration={6000} onClose={() => setSnackbar(prev => ({...prev, open: false}))} anchorOrigin={{ vertical: 'top', horizontal: 'center' }}>
                <Alert onClose={() => setSnackbar(prev => ({...prev, open: false}))} severity={snackbar.severity} variant="filled" sx={{ width: '100%' }}>{snackbar.message}</Alert>
            </Snackbar>
        </>
    );
};

export default EstrategiaForm;