import React, { useState, useEffect, useReducer } from 'react';
import { Box, Button, CircularProgress, Alert, Snackbar, Stepper, Step, StepButton } from '@mui/material';
import axios from 'axios';

import Step1_InfoGerais from './formSteps/Step1_InfoGerais';
import Step2_ParametrosAnalise from './formSteps/Step2_ParametrosAnalise';
import Step3_RegrasCompra from './formSteps/Step3_RegrasCompra';
import Step4_RegrasVenda from './formSteps/Step4_RegrasVenda';
import Step5_Revisao from './formSteps/Step5_Revisao';

import { getEstrategiaById, createEstrategia, updateEstrategia } from '../../services/estrategiaService';
import type { CondicaoCompraDTO, CondicaoVendaDTO, CriarEstrategiaDTO } from '../../types/estrategia';
import { TipoIndicador } from '../../types/enums';
import { formatLeadingZeros } from "../../utils/inputFormatters.ts";

interface EstrategiaFormProps {
    entityId: string | null;
    onClose: () => void;
    onSave: () => void;
}

type EstrategiaFormData = Omit<CriarEstrategiaDTO,
    'periodoRsiCurto' | 'periodoRsiMedio' | 'periodoRsiLongo' | 'periodoRsiEstocastico' |
    'suavizacaoRsiEstocasticoK' | 'suavizacaoRsiEstocasticoD' | 'periodoEma' | 'periodoSma' |
    'valorOperacaoFixo' | 'percentualValorOperacao' | 'percentualLucro'
> & {
    periodoRsiCurto?: string;
    periodoRsiMedio?: string;
    periodoRsiLongo?: string;
    periodoRsiEstocastico?: string;
    suavizacaoRsiEstocasticoK?: string;
    suavizacaoRsiEstocasticoD?: string;
    periodoEma?: string;
    periodoSma?: string;
    valorOperacaoFixo?: string;
    percentualValorOperacao?: string;
    percentualLucro?: string;
};

const steps = ['Informações Gerais', 'Parâmetros de Análise', 'Regras de Compra', 'Regras de Venda', 'Revisão'];

const getInitialState = (): EstrategiaFormData => ({
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
    tipoMoedaValorOperacao: 'QUOTE',
});

// --- REDUCER LOGIC ---
type Action =
    | { type: 'SET_FORM_DATA'; payload: EstrategiaFormData }
    | { type: 'SET_FIELD'; field: string; value: any }
    | { type: 'SET_VALOR_OPERACAO'; name: 'valorOperacaoFixo' | 'percentualValorOperacao'; value: string | undefined }
    | { type: 'ADD_CONDICAO'; tipo: 'compra' | 'venda' }
    | { type: 'REMOVE_CONDICAO'; tipo: 'compra' | 'venda'; index: number }
    | { type: 'UPDATE_CONDICAO'; tipo: 'compra' | 'venda'; index: number; payload: CondicaoCompraDTO | CondicaoVendaDTO };

function estrategiaReducer(state: EstrategiaFormData, action: Action): EstrategiaFormData {
    switch (action.type) {
        case 'SET_FORM_DATA':
            return action.payload;
        case 'SET_FIELD':
            return { ...state, [action.field]: action.value };
        case 'SET_VALOR_OPERACAO': {
            const newState = { ...state, [action.name]: action.value };
            if (action.name === 'valorOperacaoFixo' && action.value) {
                newState.percentualValorOperacao = undefined;
                if (!newState.tipoMoedaValorOperacao) newState.tipoMoedaValorOperacao = 'QUOTE';
            }
            if (action.name === 'percentualValorOperacao' && action.value) {
                newState.valorOperacaoFixo = undefined;
                newState.tipoMoedaValorOperacao = undefined;
            }
            return newState;
        }
        case 'ADD_CONDICAO': {
            const field = action.tipo === 'compra' ? 'condicoesCompra' : 'condicoesVenda';
            const newCondicao = {
                clientId: Math.random(),
                tipoIndicador: TipoIndicador.RSI_CURTO,
                ordem: state[field].length,
                operadorParaProxima: 'AND'
            };
            return { ...state, [field]: [...state[field], newCondicao as any] };
        }
        case 'REMOVE_CONDICAO': {
            const field = action.tipo === 'compra' ? 'condicoesCompra' : 'condicoesVenda';
            const newCondicoes = state[field].filter((_, i) => i !== action.index);
            const reorderedCondicoes = newCondicoes.map((cond, newIndex) => ({
                ...cond,
                ordem: newIndex,
            }));
            return { ...state, [field]: reorderedCondicoes };
        }
        case 'UPDATE_CONDICAO': {
            const field = action.tipo === 'compra' ? 'condicoesCompra' : 'condicoesVenda';
            const newCondicoes = [...state[field]];
            newCondicoes[action.index] = action.payload as any;
            return { ...state, [field]: newCondicoes };
        }
        default:
            return state;
    }
}
// --- END OF REDUCER LOGIC ---

const isIndicatorEnabled = (indicator: TipoIndicador, estrategia: EstrategiaFormData): boolean => {
    switch (indicator) {
        case TipoIndicador.RSI_CURTO: return estrategia.utilizarRsiCurto;
        case TipoIndicador.RSI_MEDIO: return estrategia.utilizarRsiMedio;
        case TipoIndicador.RSI_LONGO: return estrategia.utilizarRsiLongo;
        case TipoIndicador.RSI_ESTOCASTICO_K:
        case TipoIndicador.RSI_ESTOCASTICO_D: return estrategia.utilizarRsiEstocastico;
        case TipoIndicador.EMA: return estrategia.utilizarEma;
        case TipoIndicador.SMA: return estrategia.utilizarSma;
        case TipoIndicador.VOLUME: return estrategia.realizarLeituraVolume;
        default: return false;
    }
};

const EstrategiaForm: React.FC<EstrategiaFormProps> = ({ entityId, onClose, onSave }) => {
    const isEditMode = !!entityId;
    const [activeStep, setActiveStep] = useState(0);
    const [estrategia, dispatch] = useReducer(estrategiaReducer, getInitialState());
    const [loading, setLoading] = useState<boolean>(false);
    const [snackbar, setSnackbar] = useState<{ open: boolean; message: string; severity: 'success' | 'error' }>({ open: false, message: '', severity: 'error' });
    const [errors, setErrors] = useState<Record<string, string | null>>({});

    useEffect(() => {
        if (isEditMode) {
            setLoading(true);
            getEstrategiaById(entityId)
                .then(data => {
                    const formData: any = { ...data };
                    Object.keys(formData).forEach(key => {
                        if (typeof formData[key] === 'number') formData[key] = String(formData[key]);
                    });
                    const processedData = {
                        ...formData,
                        condicoesCompra: data.condicoesCompra.map(c => ({...c, clientId: Math.random()})).sort((a,b) => a.ordem - b.ordem),
                        condicoesVenda: data.condicoesVenda.map(v => ({...v, clientId: Math.random()})).sort((a,b) => a.ordem - b.ordem)
                    };
                    dispatch({ type: 'SET_FORM_DATA', payload: processedData });
                })
                .catch(err => {
                    console.error("Falha ao carregar estratégia:", err);
                    setSnackbar({ open: true, message: 'Falha ao carregar dados para edição.', severity: 'error' });
                })
                .finally(() => setLoading(false));
        }
    }, [entityId, isEditMode]);

    const handleMainChange = (name: string, value: any) => {
        if (errors[name]) setErrors(prev => ({ ...prev, [name]: null }));
        const processedValue = typeof value === 'string' ? formatLeadingZeros(value) : value;
        dispatch({ type: 'SET_FIELD', field: name, value: processedValue });
    };

    const handleValorOperacaoChange = (name: 'valorOperacaoFixo' | 'percentualValorOperacao', value: string | undefined) => {
        if (errors.valorOperacaoFixo || errors.percentualValorOperacao) setErrors(prev => ({ ...prev, valorOperacaoFixo: null, percentualValorOperacao: null, tipoMoedaValorOperacao: null }));
        const formattedValue = value !== undefined ? formatLeadingZeros(value) : undefined;
        dispatch({ type: 'SET_VALOR_OPERACAO', name, value: formattedValue });
    };

    const handlePercentChange = (name: 'percentualValorOperacao' | 'percentualLucro', value: string, max: number) => {
        const formattedValue = formatLeadingZeros(value);
        if (name === 'percentualValorOperacao' && (errors.valorOperacaoFixo || errors.percentualValorOperacao)) setErrors(prev => ({ ...prev, valorOperacaoFixo: null, percentualValorOperacao: null }));
        if (formattedValue === '') { handleMainChange(name, ''); return; }
        const numValue = Number(formattedValue);
        let finalValue = numValue;
        if (numValue < 0) finalValue = 0;
        if (numValue > max) finalValue = max;
        const finalValueStr = String(finalValue);
        if (name === 'percentualValorOperacao') handleValorOperacaoChange(name, finalValueStr);
        else handleMainChange(name, finalValueStr);
    };

    const addCondicaoCompra = () => dispatch({ type: 'ADD_CONDICAO', tipo: 'compra' });
    const updateCondicaoCompra = (index: number, updated: CondicaoCompraDTO) => dispatch({ type: 'UPDATE_CONDICAO', tipo: 'compra', index, payload: updated });
    const removeCondicaoCompra = (index: number) => dispatch({ type: 'REMOVE_CONDICAO', tipo: 'compra', index });
    const addCondicaoVenda = () => dispatch({ type: 'ADD_CONDICAO', tipo: 'venda' });
    const updateCondicaoVenda = (index: number, updated: CondicaoVendaDTO) => dispatch({ type: 'UPDATE_CONDICAO', tipo: 'venda', index, payload: updated });
    const removeCondicaoVenda = (index: number) => dispatch({ type: 'REMOVE_CONDICAO', tipo: 'venda', index });

    const validateStep = (step: number): boolean => {
        const newErrors: Record<string, string | null> = {};
        let stepIsValid = true;

        if (step === 0) { // Validação do Nome
            if (!estrategia.nome.trim()) {
                newErrors.nome = 'O nome da estratégia é obrigatório.';
                stepIsValid = false;
            }
        } else if (step === 2) { // Validação das Regras de Compra
            if (estrategia.condicoesCompra.length > 0) {
                const valorFixo = estrategia.valorOperacaoFixo;
                const valorPercentual = estrategia.percentualValorOperacao;
                if ((!valorFixo || Number(valorFixo) <= 0) && (!valorPercentual || Number(valorPercentual) <= 0)) {
                    newErrors.valorOperacaoFixo = "Defina um valor de operação.";
                    stepIsValid = false;
                }
                if (valorFixo && Number(valorFixo) > 0 && !estrategia.tipoMoedaValorOperacao) {
                    newErrors.tipoMoedaValorOperacao = 'Selecione o tipo de moeda.';
                    stepIsValid = false;
                }
            }
            estrategia.condicoesCompra.forEach((cond) => {
                if (!isIndicatorEnabled(cond.tipoIndicador, estrategia)) stepIsValid = false;
            });
        } else if (step === 3) { // Validação das Regras de Venda
            estrategia.condicoesVenda.forEach((cond) => {
                if (!isIndicatorEnabled(cond.tipoIndicador, estrategia)) stepIsValid = false;
            });
        }
        setErrors(prev => ({...prev, ...newErrors}));
        return stepIsValid;
    };

    const handleNext = () => {
        if (validateStep(activeStep)) {
            setActiveStep((prev) => prev + 1);
        } else {
            setSnackbar({ open: true, message: 'Corrija os erros ou inconsistências para avançar.', severity: 'error' });
        }
    };
    const handleBack = () => setActiveStep((prev) => prev - 1);
    const handleStep = (step: number) => () => setActiveStep(step);

    const handleSubmit = async () => {
        if (!validateStep(0) || !validateStep(2) || !validateStep(3)) {
            setSnackbar({ open: true, message: 'Existem erros no formulário. Por favor, revise todas as etapas.', severity: 'error' });
            return;
        }
        setLoading(true);
        const numberFields: (keyof CriarEstrategiaDTO)[] = ['periodoRsiCurto', 'periodoRsiMedio', 'periodoRsiLongo', 'periodoRsiEstocastico', 'suavizacaoRsiEstocasticoK', 'suavizacaoRsiEstocasticoD', 'periodoEma', 'periodoSma', 'valorOperacaoFixo', 'percentualValorOperacao', 'percentualLucro'];
        const payload = { ...estrategia };
        for (const field of numberFields) {
            const value = payload[field];
            if (value !== null && value !== undefined && value !== '') (payload as any)[field] = Number(value);
            else (payload as any)[field] = null;
        }
        try {
            if (isEditMode) await updateEstrategia(entityId, payload as CriarEstrategiaDTO);
            else await createEstrategia(payload as CriarEstrategiaDTO);
            onSave();
        } catch (error) {
            let message = 'Ocorreu um erro inesperado.';
            if (axios.isAxiosError(error) && error.response) message = error.response.data.message || message;
            else if (error instanceof Error) message = error.message;
            setSnackbar({ open: true, message, severity: 'error' });
        } finally {
            setLoading(false);
        }
    };

    const getStepContent = (step: number) => {
        switch (step) {
            case 0:
                return <Step1_InfoGerais formData={estrategia} handleMainChange={handleMainChange} errors={errors} />;
            case 1:
                return <Step2_ParametrosAnalise formData={estrategia} handleMainChange={handleMainChange} />;
            case 2:
                return <Step3_RegrasCompra estrategia={estrategia} errors={errors} addCondicaoCompra={addCondicaoCompra} updateCondicaoCompra={updateCondicaoCompra} removeCondicaoCompra={removeCondicaoCompra} handleValorOperacaoChange={handleValorOperacaoChange} handlePercentChange={handlePercentChange} handleMainChange={handleMainChange} />;
            case 3:
                return <Step4_RegrasVenda estrategia={estrategia} handleMainChange={handleMainChange} handlePercentChange={handlePercentChange} addCondicaoVenda={addCondicaoVenda} updateCondicaoVenda={updateCondicaoVenda} removeCondicaoVenda={removeCondicaoVenda} />;
            case 4:
                return <Step5_Revisao estrategia={estrategia} />;
            default:
                return 'Passo desconhecido';
        }
    };

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
            <Stepper nonLinear activeStep={activeStep} sx={{ px: 2, pt: 2, mb: 3 }}>
                {steps.map((label, index) => (
                    <Step key={label}>
                        <StepButton color="inherit" onClick={handleStep(index)}>
                            {label}
                        </StepButton>
                    </Step>
                ))}
            </Stepper>
            <Box sx={{ flexGrow: 1, overflow: 'auto', px: 4, py: 2, position: 'relative' }}>
                {loading ? <CircularProgress sx={{ position: 'absolute', top: '50%', left: '50%' }}/> : getStepContent(activeStep)}
            </Box>
            <Box sx={{ p: 2, borderTop: 1, borderColor: 'divider' }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Button variant="outlined" onClick={onClose}>Cancelar</Button>
                    <Box>
                        <Button color="inherit" disabled={activeStep === 0 || loading} onClick={handleBack} sx={{ mr: 1 }}>Voltar</Button>
                        <Button variant="contained" onClick={activeStep === steps.length - 1 ? handleSubmit : handleNext} disabled={loading}>
                            {activeStep === steps.length - 1 ? 'Salvar' : 'Avançar'}
                        </Button>
                    </Box>
                </Box>
            </Box>
            <Snackbar open={snackbar.open} autoHideDuration={6000} onClose={() => setSnackbar(prev => ({...prev, open: false}))} anchorOrigin={{ vertical: 'top', horizontal: 'center' }}>
                <Alert onClose={() => setSnackbar(prev => ({...prev, open: false}))} severity={snackbar.severity} variant="filled" sx={{ width: '100%' }}>{snackbar.message}</Alert>
            </Snackbar>
        </Box>
    );
};

export default EstrategiaForm;