import { useState, useEffect, useCallback } from 'react';
import { Box, Button, CircularProgress, Alert, Snackbar, Stepper, Step, StepButton, StepLabel } from '@mui/material';
import axios from "axios";

import type { CriarOperacaoDTO } from '../../types/operacao';
import type { SymbolInfo } from '../../types/mexc';
import type { Estrategia } from '../../types/estrategia';
import { getOperacaoById, createOperacao, updateOperacao } from "../../services/operacaoService";
import { getStablecoinPairs } from "../../services/mexcService";
import { getEstrategiaById } from "../../services/estrategiaService";

import Step1_SelecaoPar from './formSteps/Step1_SelecaoPar';
import Step2_ConfiguracaoOperacao from './formSteps/Step2_ConfiguracaoOperacao';
import Step3_RevisaoOperacao from './formSteps/Step3_RevisaoOperacao';

interface OperacaoFormProps {
    entityId: string | null;
    onClose: () => void;
    onSave: () => void;
}

const steps = ['Seleção do Par', 'Configuração', 'Revisão'];

export default function OperacaoForm({ entityId, onClose, onSave }: OperacaoFormProps) {
    const isEditMode = !!entityId;
    const [activeStep, setActiveStep] = useState(0);
    const [formData, setFormData] = useState<Partial<CriarOperacaoDTO>>({ intervalo: '15m' });
    const [errors, setErrors] = useState<Record<string, string | null>>({});
    const [loading, setLoading] = useState(true);
    const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'error' as const });
    const [stepErrors, setStepErrors] = useState<boolean[]>([false, false, false]);

    const [allPairs, setAllPairs] = useState<SymbolInfo[]>([]);
    const [selectedEstrategia, setSelectedEstrategia] = useState<Estrategia | null>(null);

    useEffect(() => {
        const fetchData = async () => {
            setLoading(true);
            try {
                const pairsData = await getStablecoinPairs();
                setAllPairs(pairsData);
                if (isEditMode && entityId) {
                    const operacaoData = await getOperacaoById(entityId);
                    setFormData({
                        par: operacaoData.par,
                        intervalo: operacaoData.intervalo,
                        idOperador: operacaoData.operador.id,
                        idEstrategia: operacaoData.estrategia.id
                    });
                    setSelectedEstrategia(operacaoData.estrategia);
                }
            } catch (err) {
                console.error("Falha ao carregar dados iniciais:", err);
                setSnackbar({ open: true, message: 'Não foi possível carregar os dados necessários.', severity: 'error' });
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, [entityId, isEditMode]);

    const handleChange = (name: string, value: any) => {
        setFormData(prev => ({ ...prev, [name]: value }));
        if (errors[name]) {
            setErrors(prev => ({ ...prev, [name]: null }));
        }
    };

    const handleEstrategiaChange = useCallback(async (estrategiaId: string | null) => {
        handleChange('idEstrategia', estrategiaId);
        if (estrategiaId) {
            try {
                const estrategiaData = await getEstrategiaById(estrategiaId);
                setSelectedEstrategia(estrategiaData);
                if (estrategiaData.valorOperacaoFixo && estrategiaData.tipoMoedaValorOperacao === 'BASE') {
                    setErrors(prev => ({ ...prev, idEstrategia: 'Incompatível: a estratégia usa MOEDA BASE para valor fixo.' }));
                } else {
                    setErrors(prev => ({ ...prev, idEstrategia: null }));
                }
            } catch (error) {
                console.error("Falha ao buscar detalhes da estratégia:", error);
                setSelectedEstrategia(null);
            }
        } else {
            setSelectedEstrategia(null);
            setErrors(prev => ({ ...prev, idEstrategia: null }));
        }
    }, []);

    const validateAllSteps = (): boolean => {
        const newErrors: Record<string, string | null> = {};
        const newStepErrors = [...stepErrors].fill(false);
        let formIsValid = true;

        // Etapa 1: Seleção do Par
        if (!formData.par) {
            newErrors.par = 'Selecione um par de moedas.';
            newStepErrors[0] = true;
            formIsValid = false;
        }

        // Etapa 2: Configuração
        if (!formData.intervalo) {
            newErrors.intervalo = 'Intervalo é obrigatório.';
            newStepErrors[1] = true;
            formIsValid = false;
        }
        if (!formData.idOperador) {
            newErrors.idOperador = 'Operador é obrigatório.';
            newStepErrors[1] = true;
            formIsValid = false;
        }
        if (!formData.idEstrategia) {
            newErrors.idEstrategia = 'Estratégia é obrigatória.';
            newStepErrors[1] = true;
            formIsValid = false;
        }
        if (errors.idEstrategia) { // Revalida erro assíncrono
            newErrors.idEstrategia = errors.idEstrategia;
            newStepErrors[1] = true;
            formIsValid = false;
        }

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
        try {
            const payload = formData as CriarOperacaoDTO;
            if (isEditMode) await updateOperacao(entityId, payload);
            else await createOperacao(payload);
            onSave();
        } catch (error) {
            const message = axios.isAxiosError(error) && error.response ? error.response.data.message : 'Ocorreu um erro inesperado.';
            setSnackbar({ open: true, message, severity: 'error' });
        } finally {
            setLoading(false);
        }
    };

    const getStepContent = (step: number) => {
        switch (step) {
            case 0:
                return <Step1_SelecaoPar formData={formData} errors={errors} allPairs={allPairs} onPairChange={(par) => handleChange('par', par)} />;
            case 1:
                return <Step2_ConfiguracaoOperacao formData={formData} errors={errors} onFieldChange={handleChange} onEstrategiaChange={handleEstrategiaChange} />;
            case 2:
                return <Step3_RevisaoOperacao formData={formData} selectedEstrategia={selectedEstrategia} />;
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
                    <Button variant="outlined" onClick={onClose} disabled={loading}>Cancelar</Button>
                    <Box>
                        <Button color="inherit" disabled={activeStep === 0 || loading} onClick={handleBack} sx={{ mr: 1 }}>Voltar</Button>
                        <Button variant="contained" onClick={activeStep === steps.length - 1 ? handleSubmit : handleNext} disabled={loading}>
                            {activeStep === steps.length - 1 ? 'Salvar' : 'Avançar'}
                        </Button>
                    </Box>
                </Box>
            </Box>

            <Snackbar open={snackbar.open} autoHideDuration={6000} onClose={() => setSnackbar(prev => ({...prev, open: false}))} anchorOrigin={{ vertical: 'top', horizontal: 'center' }}>
                <Alert onClose={() => setSnackbar(prev => ({...prev, open: false}))} severity={snackbar.severity} variant="filled" sx={{ width: '100%' }}>
                    {snackbar.message}
                </Alert>
            </Snackbar>
        </Box>
    );
}