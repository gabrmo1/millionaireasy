import React, { useState, useEffect, useReducer } from 'react';
import { Box, Button, CircularProgress, Alert, Snackbar, Stepper, Step, StepButton, StepLabel } from '@mui/material';
import axios from 'axios';

import Step1_InfoGerais from './formSteps/Step1_InfoGerais';
import Step2_ParametrosAnalise from './formSteps/Step2_ParametrosAnalise';
import Step3_RegrasCompra from './formSteps/Step3_RegrasCompra';
import Step4_RegrasVenda from './formSteps/Step4_RegrasVenda';
import Step5_Revisao from './formSteps/Step5_Revisao';

import { getEstrategiaById, createEstrategia, updateEstrategia } from '../../services/estrategiaService';
import { getStablecoins } from '../../services/mexcService';
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
});

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
            }
            if (action.name === 'percentualValorOperacao' && action.value) {
                newState.valorOperacaoFixo = undefined;
                newState.stablecoin = undefined;
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
    const [stepErrors, setStepErrors] = useState<boolean[]>([false, false, false, false, false]);
    const [stablecoins, setStablecoins] = useState<string[]>([]);

    useEffect(() => {
        getStablecoins().then(setStablecoins).catch(err => {
            console.error("Falha ao buscar stablecoins:", err);
            setSnackbar({ open: true, message: 'Falha ao carregar lista de stablecoins.', severity: 'error' });
        });

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
        if (errors.valorOperacaoFixo || errors.percentualValorOperacao || errors.stablecoin) {
            setErrors(prev => ({ ...prev, valorOperacaoFixo: null, percentualValorOperacao: null, stablecoin: null }));
        }
        const formattedValue = value !== undefined ? formatLeadingZeros(value) : undefined;
        dispatch({ type: 'SET_VALOR_OPERACAO', name, value: formattedValue });
    };

    const handlePercentChange = (name: 'percentualValorOperacao' | 'percentualLucro', value: string, max: number) => {
        const formattedValue = formatLeadingZeros(value);
        if (name === 'percentualValorOperacao' && (errors.valorOperacaoFixo || errors.percentualValorOperacao)) {
            setErrors(prev => ({ ...prev, valorOperacaoFixo: null, percentualValorOperacao: null }));
        }
        if (name === 'percentualLucro' && errors.percentualLucro) {
            setErrors(prev => ({...prev, percentualLucro: null}));
        }
        if (formattedValue === '') {
            handleMainChange(name, '');
            return;
        }
        const numValue = Number(formattedValue);
        let finalValue = numValue;
        if (numValue < 0) finalValue = 0;
        if (numValue > max) finalValue = max;
        const finalValueStr = String(finalValue);
        if (name === 'percentualValorOperacao') {
            handleValorOperacaoChange(name, finalValueStr);
        } else {
            handleMainChange(name, finalValueStr);
        }
    };

    const addCondicaoCompra = () => dispatch({ type: 'ADD_CONDICAO', tipo: 'compra' });
    const updateCondicaoCompra = (index: number, updated: CondicaoCompraDTO) => dispatch({ type: 'UPDATE_CONDICAO', tipo: 'compra', index, payload: updated });
    const removeCondicaoCompra = (index: number) => dispatch({ type: 'REMOVE_CONDICAO', tipo: 'compra', index });
    const addCondicaoVenda = () => dispatch({ type: 'ADD_CONDICAO', tipo: 'venda' });
    const updateCondicaoVenda = (index: number, updated: CondicaoVendaDTO) => dispatch({ type: 'UPDATE_CONDICAO', tipo: 'venda', index, payload: updated });
    const removeCondicaoVenda = (index: number) => dispatch({ type: 'REMOVE_CONDICAO', tipo: 'venda', index });

    const validateAllSteps = (): boolean => {
        const newErrors: Record<string, string | null> = {};
        const newStepErrors = [...stepErrors].fill(false);
        let formIsValid = true;

        // Etapa 1: Informações Gerais
        if (!estrategia.nome.trim()) {
            newErrors.nome = 'O nome da estratégia é obrigatório.';
            newStepErrors[0] = true;
            formIsValid = false;
        }

        // Etapa 3: Regras de Compra
        const hasValorFixo = estrategia.valorOperacaoFixo && Number(estrategia.valorOperacaoFixo) > 0;
        const hasValorPercentual = estrategia.percentualValorOperacao && Number(estrategia.percentualValorOperacao) > 0;

        if (estrategia.condicoesCompra.length > 0) {
            if (!hasValorFixo && !hasValorPercentual) {
                newErrors.valorOperacaoFixo = "Defina um valor de operação (fixo ou percentual).";
                newStepErrors[2] = true;
                formIsValid = false;
            }
        }

        if ((hasValorFixo || hasValorPercentual) && !estrategia.stablecoin) {
            newErrors.stablecoin = 'Selecione a Stablecoin, pois um valor de operação foi definido.';
            newStepErrors[2] = true;
            formIsValid = false;
        }

        estrategia.condicoesCompra.forEach((cond) => {
            if (!isIndicatorEnabled(cond.tipoIndicador, estrategia)) {
                newStepErrors[2] = true;
                formIsValid = false;
            }
        });

        // Etapa 4: Regras de Venda
        if (estrategia.vendaApenasPorLucro && (!estrategia.percentualLucro || Number(estrategia.percentualLucro) <= 0)) {
            newErrors.percentualLucro = 'O percentual de lucro deve ser maior que zero.';
            newStepErrors[3] = true;
            formIsValid = false;
        }
        estrategia.condicoesVenda.forEach((cond) => {
            if (!isIndicatorEnabled(cond.tipoIndicador, estrategia)) {
                newStepErrors[3] = true;
                formIsValid = false;
            }
        });

        setErrors(newErrors);
        setStepErrors(newStepErrors);
        return formIsValid;
    };

    const handleNext = () => setActiveStep((prev) => prev + 1);
    const handleBack = () => setActiveStep((prev) => prev - 1);
    const handleStep = (step: number) => () => setActiveStep(step);

    const handleSubmit = async () => {
        if (!validateAllSteps()) {
            setSnackbar({ open: true, message: 'Existem erros no formulário. Verifique as etapas marcadas em vermelho.', severity: 'error' });
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
                return <Step3_RegrasCompra stablecoins={stablecoins} estrategia={estrategia} errors={errors} addCondicaoCompra={addCondicaoCompra} updateCondicaoCompra={updateCondicaoCompra} removeCondicaoCompra={removeCondicaoCompra} handleValorOperacaoChange={handleValorOperacaoChange} handlePercentChange={handlePercentChange} handleMainChange={handleMainChange} />;
            case 3:
                return <Step4_RegrasVenda estrategia={estrategia} errors={errors} handleMainChange={handleMainChange} handlePercentChange={handlePercentChange} addCondicaoVenda={addCondicaoVenda} updateCondicaoVenda={updateCondicaoVenda} removeCondicaoVenda={removeCondicaoVenda} />;
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
                    <Step key={label} completed={false}>
                        <StepButton color="inherit" onClick={handleStep(index)}>
                            <StepLabel error={stepErrors[index]}>{label}</StepLabel>
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