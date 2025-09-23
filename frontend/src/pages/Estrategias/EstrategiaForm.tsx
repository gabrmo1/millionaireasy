import React, { useState, useEffect } from 'react';
import { Box, Button, CircularProgress, Alert, Snackbar, Stepper, Step, StepButton, StepLabel } from '@mui/material';
import axios from 'axios';

// Importando os componentes de cada etapa
import Step1_InfoGerais from './formSteps/Step1_InfoGerais';
import Step2_Indicadores from './formSteps/Step2_Indicadores';
import Step3_RegrasCompra from './formSteps/Step3_RegrasCompra';
import Step4_RegrasVenda from './formSteps/Step4_RegrasVenda';
import Step5_Revisao from './formSteps/Step5_Revisao';

import { getEstrategiaById, createEstrategia, updateEstrategia } from '../../services/estrategiaService';
import { getStablecoins } from '../../services/mexcService';
import type { CriarEstrategiaDTO, IndicadorConfigDTO, CondicaoDTO } from '../../types/estrategia';
import { TipoOperando, OperadorComparacao } from '../../types/enums';
import { formatLeadingZeros } from "../../utils/inputFormatters.ts";
import { generateIndicatorAlias } from './estrategiaUtils';

interface EstrategiaFormProps {
    entityId: string | null;
    onClose: () => void;
    onSave: () => void;
}

type EstrategiaFormData = Omit<CriarEstrategiaDTO, 'valorOperacaoFixo' | 'percentualValorOperacao' | 'percentualLucro'> & {
    valorOperacaoFixo?: string;
    percentualValorOperacao?: string;
    percentualLucro?: string;
};

const steps = ['Informações', 'Indicadores', 'Regras de Compra', 'Regras de Venda', 'Revisão'];

const getInitialState = (): EstrategiaFormData => ({
    nome: '',
    stablecoin: 'USDT',
    indicadoresConfig: [],
    condicoesCompra: [],
    condicoesVenda: [],
});

const EstrategiaForm: React.FC<EstrategiaFormProps> = ({ entityId, onClose, onSave }) => {
    const isEditMode = !!entityId;
    const [activeStep, setActiveStep] = useState(0);
    const [formData, setFormData] = useState<EstrategiaFormData>(getInitialState());
    const [loading, setLoading] = useState<boolean>(false);
    const [snackbar, setSnackbar] = useState<{ open: boolean; message: string; severity: 'success' | 'error' }>({ open: false, message: '', severity: 'error' });
    const [errors, setErrors] = useState<Record<string, string | null>>({});
    const [stepErrors, setStepErrors] = useState<boolean[]>([false, false, false, false, false]);
    const [stablecoins, setStablecoins] = useState<string[]>([]);

    useEffect(() => {
        getStablecoins().then(setStablecoins).catch(err => {
            console.error("Falha ao buscar stablecoins:", err);
        });

        if (isEditMode && entityId) {
            setLoading(true);
            getEstrategiaById(entityId)
                .then(data => {
                    const loadedData: EstrategiaFormData = {
                        ...data,
                        indicadoresConfig: data.indicadoresConfig.map(i => ({...i, clientId: Math.random()})),
                        condicoesCompra: data.condicoesCompra.map(c => ({...c, clientId: Math.random()})).sort((a,b) => a.ordem - b.ordem),
                        condicoesVenda: data.condicoesVenda.map(v => ({...v, clientId: Math.random()})).sort((a,b) => a.ordem - b.ordem),
                        valorOperacaoFixo: data.valorOperacaoFixo?.toString(),
                        percentualValorOperacao: data.percentualValorOperacao?.toString(),
                        percentualLucro: data.percentualLucro?.toString(),
                    };
                    setFormData(loadedData);
                })
                .catch(err => console.error("Falha ao carregar estratégia:", err))
                .finally(() => setLoading(false));
        }
    }, [entityId, isEditMode]);

    const handleFieldChange = (name: string, value: any) => {
        if (errors[name]) setErrors(prev => ({ ...prev, [name]: null }));
        const processedValue = typeof value === 'string' ? formatLeadingZeros(value) : value;
        setFormData(prev => ({ ...prev, [name]: processedValue }));
    };

    const addIndicador = () => {
        const newIndicador: IndicadorConfigDTO = {
            clientId: Math.random(),
            alias: '',
            tipoIndicador: 'RSI_CURTO',
            parametros: { periodoRsiCurto: 7 }
        };
        newIndicador.alias = generateIndicatorAlias(newIndicador);
        setFormData(prev => ({ ...prev, indicadoresConfig: [...prev.indicadoresConfig, newIndicador] }));
    };

    const updateIndicador = (index: number, updated: IndicadorConfigDTO) => {
        const newIndicadores = [...formData.indicadoresConfig];
        // --- LÓGICA CENTRALIZADA ---
        // Regenera o alias sempre que o indicador for atualizado (tipo ou parâmetros).
        updated.alias = generateIndicatorAlias(updated);
        newIndicadores[index] = updated;
        setFormData(prev => ({ ...prev, indicadoresConfig: newIndicadores }));
    };

    const removeIndicador = (index: number) => {
        const aliasToRemove = formData.indicadoresConfig[index].alias;
        setFormData(prev => ({
            ...prev,
            indicadoresConfig: prev.indicadoresConfig.filter((_, i) => i !== index),
            condicoesCompra: prev.condicoesCompra.filter(c => c.operandoAReferencia !== aliasToRemove && c.operandoBReferencia !== aliasToRemove),
            condicoesVenda: prev.condicoesVenda.filter(c => c.operandoAReferencia !== aliasToRemove && c.operandoBReferencia !== aliasToRemove)
        }));
    };

    const addCondicao = (tipo: 'compra' | 'venda') => {
        const field = tipo === 'compra' ? 'condicoesCompra' : 'condicoesVenda';
        const newCondicao: CondicaoDTO = {
            clientId: Math.random(),
            ordem: formData[field].length,
            operadorParaProxima: 'AND',
            operandoATipo: TipoOperando.INDICADOR,
            operador: OperadorComparacao.MAIOR_QUE,
            operandoBTipo: TipoOperando.VALOR_FIXO,
        };
        setFormData(prev => ({ ...prev, [field]: [...prev[field], newCondicao] }));
    };

    const updateCondicao = (tipo: 'compra' | 'venda', index: number, updated: CondicaoDTO) => {
        const field = tipo === 'compra' ? 'condicoesCompra' : 'condicoesVenda';
        const newCondicoes = [...formData[field]];
        newCondicoes[index] = updated;
        setFormData(prev => ({ ...prev, [field]: newCondicoes }));
    };

    const removeCondicao = (tipo: 'compra' | 'venda', index: number) => {
        const field = tipo === 'compra' ? 'condicoesCompra' : 'condicoesVenda';
        const newCondicoes = formData[field].filter((_, i) => i !== index)
            .map((cond, newIndex) => ({ ...cond, ordem: newIndex }));
        setFormData(prev => ({ ...prev, [field]: newCondicoes }));
    };

    const validateStep = (step: number, currentFormData: EstrategiaFormData): { stepErrors: Record<string, string | null>, isValid: boolean } => {
        const newErrors: Record<string, string | null> = {};
        let isValid = true;

        switch (step) {
            case 0:
                if (!currentFormData.nome.trim()) { newErrors.nome = 'O nome da estratégia é obrigatório.'; isValid = false; }
                if (!currentFormData.stablecoin) { newErrors.stablecoin = 'A stablecoin é obrigatória.'; isValid = false; }
                break;
            case 1:
                { const indicatorSignatures = new Set<string>();
                currentFormData.indicadoresConfig.forEach((indicador, index) => {
                    // Ordena as chaves dos parâmetros para criar uma assinatura consistente
                    const sortedParams = Object.keys(indicador.parametros).sort().reduce((obj, key) => {
                        obj[key] = indicador.parametros[key];
                        return obj;
                    }, {} as Record<string, number>);

                    const signature = `${indicador.tipoIndicador}-${JSON.stringify(sortedParams)}`;
                    if (indicatorSignatures.has(signature)) {
                        newErrors[`indicador_${index}_alias`] = 'Este indicador já foi adicionado com os mesmos parâmetros.';
                        isValid = false;
                    }
                    indicatorSignatures.add(signature);
                });
                break; }
            case 2:
                { const hasValor = (currentFormData.valorOperacaoFixo && Number(currentFormData.valorOperacaoFixo) > 0) || (currentFormData.percentualValorOperacao && Number(currentFormData.percentualValorOperacao) > 0);
                if (currentFormData.condicoesCompra.length > 0 && !hasValor) {
                    newErrors.valorOperacaoFixo = 'Defina um valor de operação (fixo ou percentual) para acionar as condições.';
                    isValid = false;
                }
                currentFormData.condicoesCompra.forEach((cond, index) => {
                    if (cond.operandoATipo === 'VALOR_FIXO' && cond.operandoAValor == null) {
                        newErrors[`condicao_compra_${index}_valor_a`] = 'Valor é obrigatório.';
                        isValid = false;
                    }
                    if (cond.operandoBTipo === 'VALOR_FIXO' && cond.operandoBValor == null) {
                        newErrors[`condicao_compra_${index}_valor_b`] = 'Valor é obrigatório.';
                        isValid = false;
                    }
                });
                break; }
            case 3:
                if (currentFormData.vendaApenasPorLucro && (!currentFormData.percentualLucro || Number(currentFormData.percentualLucro) <= 0)) {
                    newErrors.percentualLucro = 'O percentual de lucro deve ser maior que zero.';
                    isValid = false;
                }
                currentFormData.condicoesVenda.forEach((cond, index) => {
                    if (cond.operandoATipo === 'VALOR_FIXO' && cond.operandoAValor == null) {
                        newErrors[`condicao_venda_${index}_valor_a`] = 'Valor é obrigatório.';
                        isValid = false;
                    }
                    if (cond.operandoBTipo === 'VALOR_FIXO' && cond.operandoBValor == null) {
                        newErrors[`condicao_venda_${index}_valor_b`] = 'Valor é obrigatório.';
                        isValid = false;
                    }
                });
                break;
        }
        return { stepErrors: newErrors, isValid };
    };

    const handleNext = () => {
        setActiveStep((prev) => prev + 1);
    };

    const handleBack = () => setActiveStep((prev) => prev - 1);

    const handleStep = (step: number) => () => setActiveStep(step);

    const handleSubmit = async () => {
        let allStepsValid = true;
        let combinedErrors: Record<string, string | null> = {};
        const newStepErrors = [...stepErrors].fill(false);

        for (let i = 0; i < steps.length; i++) {
            const result = validateStep(i, formData);
            if (!result.isValid) {
                allStepsValid = false;
                newStepErrors[i] = true;
                combinedErrors = { ...combinedErrors, ...result.stepErrors };
            }
        }

        setErrors(combinedErrors);
        setStepErrors(newStepErrors);

        if (!allStepsValid) {
            setSnackbar({ open: true, message: 'Existem erros no formulário. Verifique as etapas marcadas em vermelho.', severity: 'error' });
            return;
        }

        setLoading(true);
        const payload: CriarEstrategiaDTO = {
            ...formData,
            valorOperacaoFixo: formData.valorOperacaoFixo ? Number(formData.valorOperacaoFixo) : undefined,
            percentualValorOperacao: formData.percentualValorOperacao ? Number(formData.percentualValorOperacao) : undefined,
            percentualLucro: formData.percentualLucro ? Number(formData.percentualLucro) : undefined,
        };

        try {
            if (isEditMode && entityId) {
                await updateEstrategia(entityId, payload);
            } else {
                await createEstrategia(payload);
            }
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
            case 0: return <Step1_InfoGerais formData={formData} handleFieldChange={handleFieldChange} errors={errors} stablecoins={stablecoins} />;
            case 1: return <Step2_Indicadores indicadores={formData.indicadoresConfig} onAdd={addIndicador} onRemove={removeIndicador} onUpdate={updateIndicador} errors={errors} />;
            case 2: return <Step3_RegrasCompra formData={formData} indicadores={formData.indicadoresConfig} condicoes={formData.condicoesCompra} onAdd={() => addCondicao('compra')} onRemove={(idx) => removeCondicao('compra', idx)} onUpdate={(idx, data) => updateCondicao('compra', idx, data)} handleFieldChange={handleFieldChange} errors={errors} />;
            case 3: return <Step4_RegrasVenda formData={formData} indicadores={formData.indicadoresConfig} condicoes={formData.condicoesVenda} onAdd={() => addCondicao('venda')} onRemove={(idx) => removeCondicao('venda', idx)} onUpdate={(idx, data) => updateCondicao('venda', idx, data)} handleFieldChange={handleFieldChange} errors={errors} />;
            case 4: return <Step5_Revisao formData={formData} />;
            default: return 'Passo desconhecido';
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
                {loading ? <CircularProgress sx={{ position: 'absolute', top: '50%', left: '50%' }} /> : getStepContent(activeStep)}
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

            <Snackbar open={snackbar.open} autoHideDuration={6000} onClose={() => setSnackbar(prev => ({ ...prev, open: false }))} anchorOrigin={{ vertical: 'top', horizontal: 'center' }}>
                <Alert onClose={() => setSnackbar(prev => ({ ...prev, open: false }))} severity={snackbar.severity} variant="filled" sx={{ width: '100%' }}>
                    {snackbar.message}
                </Alert>
            </Snackbar>
        </Box>
    );
};

export default EstrategiaForm;