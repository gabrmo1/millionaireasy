import React, { useState, useEffect } from 'react';
import { Box, Button, CircularProgress, Alert, Snackbar, Stepper, Step, StepButton } from '@mui/material';
import axios from 'axios';

import Step1_InfoGerais from './formSteps/Step1_InfoGerais';
import Step2_ParametrosAnalise from './formSteps/Step2_ParametrosAnalise';
import Step3_RegrasCompra from './formSteps/Step3_RegrasCompra';
import Step4_RegrasVenda from './formSteps/Step4_RegrasVenda';
import Step5_Revisao from './formSteps/Step5_Revisao';

import { getEstrategiaById, createEstrategia, updateEstrategia } from '../../services/estrategiaService';
import type { CondicaoCompraDTO, CondicaoVendaDTO, CriarEstrategiaDTO } from '../../types/estrategia';
import { estrategiaFormConfig } from './estrategiaConfig';

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

const EstrategiaForm: React.FC<EstrategiaFormProps> = ({ entityId, onClose, onSave }) => {
    const isEditMode = !!entityId;
    const [activeStep, setActiveStep] = useState(0);
    const [estrategia, setEstrategia] = useState<EstrategiaFormData>(getInitialState());
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
                        if (typeof formData[key] === 'number') {
                            formData[key] = String(formData[key]);
                        }
                    });
                    setEstrategia({ ...formData, condicoesCompra: data.condicoesCompra.map(c => ({...c, clientId: Math.random()})), condicoesVenda: data.condicoesVenda.map(v => ({...v, clientId: Math.random()})), });
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
        const fieldConfig = estrategiaFormConfig.find(f => f.name === name);
        let processedValue = value;
        if (fieldConfig && fieldConfig.type === 'number' && typeof value === 'string') {
            processedValue = formatLeadingZeros(value);
        }
        setEstrategia(prev => ({ ...prev, [name]: processedValue }));
    };

    const handleValorOperacaoChange = (name: 'valorOperacaoFixo' | 'percentualValorOperacao', value: string | undefined) => {
        if (errors.valorOperacaoFixo || errors.percentualValorOperacao) setErrors(prev => ({ ...prev, valorOperacaoFixo: null, percentualValorOperacao: null, tipoMoedaValorOperacao: null }));
        const formattedValue = value !== undefined ? formatLeadingZeros(value) : undefined;
        setEstrategia(prev => {
            const newState = { ...prev, [name]: formattedValue };
            if (name === 'valorOperacaoFixo' && formattedValue) {
                newState.percentualValorOperacao = undefined;
                if (!newState.tipoMoedaValorOperacao) newState.tipoMoedaValorOperacao = 'QUOTE';
            }
            if (name === 'percentualValorOperacao' && formattedValue) {
                newState.valorOperacaoFixo = undefined;
                newState.tipoMoedaValorOperacao = undefined;
            }
            return newState;
        });
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

    const addCondicaoCompra = () => setEstrategia(prev => ({ ...prev, condicoesCompra: [...prev.condicoesCompra, { clientId: Math.random(), tipoIndicador: 'RSI_CURTO' }] }));
    const updateCondicaoCompra = (index: number, updated: CondicaoCompraDTO) => { const newCondicoes = [...estrategia.condicoesCompra]; newCondicoes[index] = updated; setEstrategia(prev => ({ ...prev, condicoesCompra: newCondicoes })); };
    const removeCondicaoCompra = (index: number) => setEstrategia(prev => ({ ...prev, condicoesCompra: prev.condicoesCompra.filter((_, i) => i !== index) }));
    const addCondicaoVenda = () => setEstrategia(prev => ({ ...prev, condicoesVenda: [...prev.condicoesVenda, { clientId: Math.random(), tipoIndicador: 'RSI_CURTO' }] }));
    const updateCondicaoVenda = (index: number, updated: CondicaoVendaDTO) => { const newCondicoes = [...estrategia.condicoesVenda]; newCondicoes[index] = updated; setEstrategia(prev => ({ ...prev, condicoesVenda: newCondicoes })); };
    const removeCondicaoVenda = (index: number) => setEstrategia(prev => ({ ...prev, condicoesVenda: prev.condicoesVenda.filter((_, i) => i !== index) }));

    const validateForm = (): boolean => {
        const newErrors: Record<string, string | null> = {};
        if (!estrategia.nome.trim()) newErrors.nome = 'O nome da estratégia é obrigatório.';
        if (estrategia.condicoesCompra.length > 0) {
            const valorFixo = estrategia.valorOperacaoFixo;
            const valorPercentual = estrategia.percentualValorOperacao;
            if ((!valorFixo || Number(valorFixo) <= 0) && (!valorPercentual || Number(valorPercentual) <= 0)) {
                const errorMsg = "Defina um valor de operação.";
                newErrors.valorOperacaoFixo = errorMsg;
                newErrors.percentualValorOperacao = errorMsg;
            }
            if (valorFixo && Number(valorFixo) > 0 && !estrategia.tipoMoedaValorOperacao) {
                newErrors.tipoMoedaValorOperacao = 'Selecione o tipo de moeda.';
            }
        }
        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleNext = () => setActiveStep((prev) => prev + 1);
    const handleBack = () => setActiveStep((prev) => prev - 1);
    const handleStep = (step: number) => () => setActiveStep(step);

    const handleSubmit = async () => {
        if (!validateForm()) {
            setSnackbar({ open: true, message: 'Existem erros no formulário. Por favor, revise os campos.', severity: 'error' });
            return;
        }
        setLoading(true);
        const numberFields: (keyof CriarEstrategiaDTO)[] = ['periodoRsiCurto', 'periodoRsiMedio', 'periodoRsiLongo', 'periodoRsiEstocastico', 'suavizacaoRsiEstocasticoK', 'suavizacaoRsiEstocasticoD', 'periodoEma', 'periodoSma', 'valorOperacaoFixo', 'percentualValorOperacao', 'percentualLucro'];
        const payload = { ...estrategia };
        for (const field of numberFields) {
            const value = payload[field];
            if (value !== null && value !== undefined && value !== '') (payload as any)[field] = Number(value);
        }
        try {
            if (isEditMode) await updateEstrategia(entityId, payload as CriarEstrategiaDTO);
            else await createEstrategia(payload as CriarEstrategiaDTO);
            onSave();
        } catch (error) {
            console.error("Falha ao salvar estratégia:", error);
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