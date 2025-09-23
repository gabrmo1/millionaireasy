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
const initialState = { intervalo: '15m' };

export default function OperacaoForm({ entityId, onClose, onSave }: OperacaoFormProps) {
    const isEditMode = !!entityId;
    const [activeStep, setActiveStep] = useState(0);
    const [formData, setFormData] = useState<Partial<CriarOperacaoDTO>>(initialState);
    const [errors, setErrors] = useState<Record<string, string | null>>({});
    const [loading, setLoading] = useState(false);
    const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'error' as const });
    const [stepErrors, setStepErrors] = useState<boolean[]>([false, false, false]);

    const [allPairs, setAllPairs] = useState<SymbolInfo[]>([]);
    const [selectedEstrategia, setSelectedEstrategia] = useState<Estrategia | null>(null);

    // Efeito 1: Busca a lista de pares uma vez, quando o componente é montado.
    useEffect(() => {
        setLoading(true);
        getStablecoinPairs()
            .then(setAllPairs)
            .catch(err => {
                console.error("Falha ao buscar pares de moedas:", err);
                setSnackbar({ open: true, message: 'Não foi possível carregar os pares de moedas.', severity: 'error' });
            })
            .finally(() => setLoading(false));
    }, []);

    // Efeito 2: Carrega os dados para edição ou reseta o formulário para criação.
    useEffect(() => {
        if (isEditMode && entityId) {
            setLoading(true);
            getOperacaoById(entityId)
                .then(async (operacaoData) => {
                    setFormData({
                        par: operacaoData.par,
                        intervalo: operacaoData.intervalo,
                        idOperador: operacaoData.operador.id,
                        idEstrategia: operacaoData.estrategia?.id
                    });
                    const estrategiaData = await getEstrategiaById(operacaoData.estrategia.id);
                    setSelectedEstrategia(estrategiaData);
                })
                .catch(err => console.error("Falha ao carregar operação para edição:", err))
                .finally(() => setLoading(false));
        } else {
            // Reseta o estado para o modo de criação
            setFormData(initialState);
            setSelectedEstrategia(null);
            setErrors({});
            setStepErrors([false, false, false]);
            setActiveStep(0);
        }
    }, [entityId, isEditMode]);


    // Efeito 3: Validação cruzada sempre que o par ou a estratégia mudam.
    useEffect(() => {
        const validate = () => {
            if (!formData.par || !selectedEstrategia) {
                setErrors(prev => ({ ...prev, idEstrategia: null }));
                return;
            }

            const quoteAsset = allPairs.find(p => p.symbol === formData.par)?.quoteAsset;
            const estrategiaStablecoin = selectedEstrategia.stablecoin;

            if (estrategiaStablecoin && quoteAsset && estrategiaStablecoin !== quoteAsset) {
                const errorMessage = `Incompatível: O par opera com ${quoteAsset}, mas a estratégia usa ${estrategiaStablecoin}.`;
                setErrors(prev => ({ ...prev, idEstrategia: errorMessage }));
            } else {
                setErrors(prev => ({ ...prev, idEstrategia: null }));
            }
        };
        validate();
    }, [formData.par, selectedEstrategia, allPairs]);


    // --- Handlers ---
    const handlePairChange = (par: string) => {
        setFormData(prev => ({ ...prev, par: par }));
    };

    const handleEstrategiaChange = useCallback(async (estrategiaId: string | null) => {
        setFormData(prev => ({ ...prev, idEstrategia: estrategiaId || undefined }));
        if (estrategiaId) {
            try {
                const estrategiaData = await getEstrategiaById(estrategiaId);
                setSelectedEstrategia(estrategiaData);
            } catch (error) {
                console.error("Falha ao buscar detalhes da estratégia:", error);
                setSelectedEstrategia(null);
            }
        } else {
            setSelectedEstrategia(null);
        }
    }, []);

    const handleChange = (name: string, value: any) => {
        setFormData(prev => ({ ...prev, [name]: value }));
        if (errors[name] && name !== 'idEstrategia') {
            setErrors(prev => ({ ...prev, [name]: null }));
        }
    };

    const validateAllSteps = (): boolean => {
        const newErrors: Record<string, string | null> = {};
        const newStepErrors = [...stepErrors].fill(false);
        let formIsValid = true;

        if (!formData.par) {
            newErrors.par = 'Selecione um par de moedas.';
            newStepErrors[0] = true;
            formIsValid = false;
        }

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
        // if (!formData.idEstrategia) {
        //     newErrors.idEstrategia = 'Estratégia é obrigatória.';
        //     newStepErrors[1] = true;
        //     formIsValid = false;
        // }
        if (errors.idEstrategia) {
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
                return <Step1_SelecaoPar formData={formData} errors={errors} allPairs={allPairs} onPairChange={handlePairChange} />;
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