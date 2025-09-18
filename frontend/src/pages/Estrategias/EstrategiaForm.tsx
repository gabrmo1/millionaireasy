import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Paper, Typography, Box, Button, CircularProgress, Alert, Snackbar, Divider, Collapse, TextField } from '@mui/material';
import Grid from '@mui/material/GridLegacy';
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import axios from 'axios';

import { getEstrategiaById, createEstrategia, updateEstrategia } from '../../services/estrategiaService';
import type { Estrategia, CondicaoCompraDTO, CondicaoVendaDTO } from '../../types/estrategia';
import { estrategiaFormConfig } from './estrategiaConfig';

import FormFieldRenderer from '../../components/common/forms/FormFieldRenderer';
import CondicaoCompraForm from './CondicaoCompraForm';
import CondicaoVendaForm from './CondicaoVendaForm';
import {TipoIndicador} from "../../types/enums.ts";

const getInitialState = (): Omit<Estrategia, 'id'> => ({
    nome: '',
    utilizarRsiCurto: false,
    utilizarRsiMedio: false,
    utilizarRsiLongo: false,
    utilizarRsiEstocastico: false,
    utilizarEma: false,
    utilizarSma: false,
    realizarLeituraVolume: false,
    condicoesCompra: [],
    condicoesVenda: [],
});

const EstrategiaForm: React.FC = () => {
    const navigate = useNavigate();
    const { id } = useParams<{ id: string }>();
    const isEditMode = !!id;

    const [estrategia, setEstrategia] = useState<Omit<Estrategia, 'id'>>(getInitialState());
    const [loading, setLoading] = useState<boolean>(false);
    const [snackbar, setSnackbar] = useState<{ open: boolean; message: string; severity: 'success' | 'error' }>({ open: false, message: '', severity: 'error' });

    useEffect(() => {
        if (isEditMode) {
            setLoading(true);
            getEstrategiaById(id)
                .then(data => {
                    const dataWithClientIds = {
                        ...data,
                        condicoesCompra: data.condicoesCompra.map(c => ({...c, clientId: Math.random()})),
                        condicoesVenda: data.condicoesVenda.map(v => ({...v, clientId: Math.random()})),
                    };
                    setEstrategia(dataWithClientIds);
                })
                .catch(err => {
                    console.error("Falha ao carregar estratégia:", err);
                    setSnackbar({ open: true, message: 'Falha ao carregar dados para edição.', severity: 'error' });
                })
                .finally(() => setLoading(false));
        }
    }, [id, isEditMode]);

    const handleMainChange = (name: string, value: string | number | boolean) => {
        setEstrategia(prev => ({ ...prev, [name]: value }));
    };

    const handleValorOperacaoChange = (name: 'valorOperacaoFixo' | 'percentualValorOperacao', value: string) => {
        const numericValue = value ? Number(value) : undefined;

        setEstrategia(prev => {
            const newState = { ...prev, [name]: numericValue };
            if (name === 'valorOperacaoFixo' && numericValue !== undefined) {
                newState.percentualValorOperacao = undefined;
            } else if (name === 'percentualValorOperacao' && numericValue !== undefined) {
                newState.valorOperacaoFixo = undefined;
            }
            return newState;
        });
    };

    const handlePercentChange = (name: 'percentualValorOperacao', value: string) => {
        const numValue = Number(value);
        let finalValue: number | undefined = numValue;

        if (numValue < 0) finalValue = 0;
        if (numValue > 100) finalValue = 100;

        handleValorOperacaoChange(name, String(finalValue));
    };


    // --- Condições de Compra ---
    const addCondicaoCompra = () => {
        const newCondicao: CondicaoCompraDTO = { clientId: Math.random(), tipoIndicador: 'RSI_CURTO' }; // Default
        setEstrategia(prev => ({ ...prev, condicoesCompra: [...prev.condicoesCompra, newCondicao] }));
    };

    const updateCondicaoCompra = (index: number, updated: CondicaoCompraDTO) => {
        const newCondicoes = [...estrategia.condicoesCompra];
        newCondicoes[index] = updated;
        setEstrategia(prev => ({ ...prev, condicoesCompra: newCondicoes }));
    };

    const removeCondicaoCompra = (index: number) => {
        setEstrategia(prev => ({ ...prev, condicoesCompra: prev.condicoesCompra.filter((_, i) => i !== index) }));
    };

    // --- Condições de Venda ---
    const addCondicaoVenda = () => {
        const newCondicao: CondicaoVendaDTO = { clientId: Math.random(), tipoIndicador: 'RSI_CURTO' }; // Default
        setEstrategia(prev => ({ ...prev, condicoesVenda: [...prev.condicoesVenda, newCondicao] }));
    };

    const updateCondicaoVenda = (index: number, updated: CondicaoVendaDTO) => {
        const newCondicoes = [...estrategia.condicoesVenda];
        newCondicoes[index] = updated;
        setEstrategia(prev => ({ ...prev, condicoesVenda: newCondicoes }));
    };

    const removeCondicaoVenda = (index: number) => {
        setEstrategia(prev => ({ ...prev, condicoesVenda: prev.condicoesVenda.filter((_, i) => i !== index) }));
    };

    const isEstrategiaValida = (): boolean => {
        const isIndicatorEnabled = (indicator: TipoIndicador): boolean => {
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

        for (const condicao of estrategia.condicoesCompra) {
            if (!isIndicatorEnabled(condicao.tipoIndicador)) {
                setSnackbar({ open: true, message: `A Condição de Compra para ${condicao.tipoIndicador} é inválida, pois o indicador está desabilitado.`, severity: 'error' });
                return false;
            }
        }

        for (const condicao of estrategia.condicoesVenda) {
            if (!isIndicatorEnabled(condicao.tipoIndicador)) {
                setSnackbar({ open: true, message: `A Condição de Venda para ${condicao.tipoIndicador} é inválida, pois o indicador está desabilitado.`, severity: 'error' });
                return false;
            }
        }

        return true;
    };


    const handleSubmit = async (event: React.FormEvent) => {
        event.preventDefault();

        if (!isEstrategiaValida()) {
            return;
        }

        setLoading(true);
        try {
            if (isEditMode) {
                await updateEstrategia(id, estrategia);
            } else {
                await createEstrategia(estrategia);
            }
            navigate('/estrategias');
        } catch (error) {
            console.error("Falha ao salvar estratégia:", error);
            let message = 'Ocorreu um erro inesperado.';
            if (axios.isAxiosError(error) && error.response) {
                message = error.response.data.message || message;
            } else if (error instanceof Error) {
                message = error.message;
            }
            setSnackbar({ open: true, message, severity: 'error' });
        } finally {
            setLoading(false);
        }
    };

    const independentFields = estrategiaFormConfig.filter(field => !field.dependentOn);

    return (
        <>
            <Paper elevation={2} sx={{ height: 'calc(100vh - 112px)', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
                <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
                    <Typography variant="h5" component="h2">{isEditMode ? 'Editar Estratégia' : 'Criar Estratégia'}</Typography>
                </Box>

                <Box component="form" onSubmit={handleSubmit} noValidate sx={{ display: 'flex', flexDirection: 'column', flexGrow: 1, overflow: 'hidden' }}>
                    <Box sx={{ flexGrow: 1, overflow: 'auto', py: 3, px: 7, position: 'relative' }}>
                        {loading && (
                            <Box sx={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: 'rgba(255, 255, 255, 0.7)', zIndex: 1 }}>
                                <CircularProgress />
                            </Box>
                        )}

                        <Typography variant="h6" gutterBottom>Parâmetros da Análise</Typography>
                        <Grid container spacing={1.5}>
                            {independentFields.map(field => {
                                const dependentFields = estrategiaFormConfig.filter(
                                    (depField) => depField.dependentOn === field.name
                                );

                                return (
                                    <Grid item xs={12} sm={field.gridSpan ?? 12} key={field.name}>
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

                        <Divider sx={{ my: 3 }} />

                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                            <Typography variant="h6">Condições de Compra</Typography>
                            <Button startIcon={<AddCircleOutlineIcon />} onClick={addCondicaoCompra}>Adicionar</Button>
                        </Box>
                        {estrategia.condicoesCompra.map((condicao, index) => (
                            <CondicaoCompraForm
                                key={condicao.clientId}
                                index={index}
                                condicao={condicao}
                                estrategia={estrategia}
                                onUpdate={updateCondicaoCompra}
                                onRemove={removeCondicaoCompra}
                            />
                        ))}
                        {estrategia.condicoesCompra.length === 0 && <Alert severity="info">Nenhuma condição de compra adicionada.</Alert>}

                        <Divider sx={{ my: 3 }} />

                        <Box>
                            <Typography variant="h6" sx={{ mb: 2 }}>Valor da Operação</Typography>
                            <Grid container spacing={2}>
                                <Grid item xs={6}>
                                    <TextField
                                        label="Valor Fixo Operação (Ex: 10.50)"
                                        type="number"
                                        size="small"
                                        fullWidth
                                        value={estrategia.valorOperacaoFixo ?? ''}
                                        onChange={(e) => handleValorOperacaoChange('valorOperacaoFixo', e.target.value)}
                                        disabled={estrategia.percentualValorOperacao !== undefined && estrategia.percentualValorOperacao !== null}
                                        inputProps={{ min: 0 }}
                                    />
                                </Grid>
                                <Grid item xs={6}>
                                    <TextField
                                        label="% do Saldo na Operação"
                                        type="number"
                                        size="small"
                                        fullWidth
                                        value={estrategia.percentualValorOperacao ?? ''}
                                        onChange={(e) => handlePercentChange('percentualValorOperacao', e.target.value)}
                                        disabled={estrategia.valorOperacaoFixo !== undefined && estrategia.valorOperacaoFixo !== null}
                                    />
                                </Grid>
                            </Grid>
                        </Box>

                        <Divider sx={{ my: 3 }} />

                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                            <Typography variant="h6">Condições de Venda</Typography>
                            <Button startIcon={<AddCircleOutlineIcon />} onClick={addCondicaoVenda}>Adicionar</Button>
                        </Box>
                        {estrategia.condicoesVenda.map((condicao, index) => (
                            <CondicaoVendaForm
                                key={condicao.clientId}
                                index={index}
                                condicao={condicao}
                                estrategia={estrategia}
                                onUpdate={updateCondicaoVenda}
                                onRemove={removeCondicaoVenda}
                            />
                        ))}
                        {estrategia.condicoesVenda.length === 0 && <Alert severity="info">Nenhuma condição de venda adicionada.</Alert>}

                    </Box>

                    <Box sx={{ p: 2, borderTop: 1, borderColor: 'divider' }}>
                        <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
                            <Button type="submit" variant="contained" color="primary" disabled={loading}>Salvar</Button>
                            <Button variant="outlined" onClick={() => navigate('/estrategias')}>Cancelar</Button>
                        </Box>
                    </Box>
                </Box>
            </Paper>

            <Snackbar open={snackbar.open} autoHideDuration={6000} onClose={() => setSnackbar(prev => ({...prev, open: false}))} anchorOrigin={{ vertical: 'top', horizontal: 'center' }}>
                <Alert onClose={() => setSnackbar(prev => ({...prev, open: false}))} severity={snackbar.severity} variant="filled" sx={{ width: '100%' }}>
                    {snackbar.message}
                </Alert>
            </Snackbar>
        </>
    );
};

export default EstrategiaForm;