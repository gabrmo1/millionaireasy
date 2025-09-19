import React from 'react';
import { Box, Button, Typography, Divider, TextField, Collapse, FormControl, InputLabel, Select, MenuItem, Alert } from '@mui/material';
import Grid from '@mui/material/GridLegacy';
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import CondicaoCompraForm from '../CondicaoCompraForm';
import TooltipIcon from '../../../components/common/TooltipIcon';
import { getTipoMoedaValorOperacaoOptions } from '../../../utils/enumMappings';

interface Step3Props {
    estrategia: any;
    errors: Record<string, string | null>;
    addCondicaoCompra: () => void;
    updateCondicaoCompra: (index: number, updated: any) => void;
    removeCondicaoCompra: (index: number) => void;
    handleValorOperacaoChange: (name: 'valorOperacaoFixo' | 'percentualValorOperacao', value: string | undefined) => void;
    handlePercentChange: (name: 'percentualValorOperacao' | 'percentualLucro', value: string, max: number) => void;
    handleMainChange: (name: string, value: any) => void;
}

const Step3_RegrasCompra: React.FC<Step3Props> = (props) => {
    const {
        estrategia, errors, addCondicaoCompra, updateCondicaoCompra,
        removeCondicaoCompra, handleValorOperacaoChange, handleMainChange
    } = props;

    const tipoMoedaTooltip = "Moeda Base: É a primeira moeda do par (ex: BTC em BTC/USDT). Moeda de Cotação: É a segunda moeda (ex: USDT em BTC/USDT).";

    return (
        <>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6">Condições de Compra</Typography>
                <Button startIcon={<AddCircleOutlineIcon />} onClick={addCondicaoCompra}>Adicionar</Button>
            </Box>
            {estrategia.condicoesCompra.map((condicao: any, index: number) => (
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
                <Typography variant="h6" sx={{ mb: 2 }}>Parâmetros de Compra</Typography>
                <Grid container spacing={2}>
                    <Grid item xs={12} sm={4}>
                        <TextField label="Valor Fixo Operação" type="number" size="small" fullWidth value={estrategia.valorOperacaoFixo ?? ''} onChange={(e) => handleValorOperacaoChange('valorOperacaoFixo', e.target.value)} disabled={!!estrategia.percentualValorOperacao} inputProps={{ min: 0 }} error={!!errors.valorOperacaoFixo} helperText={errors.valorOperacaoFixo || ' '} />
                    </Grid>
                    <Grid item xs={12} sm={4}>
                        <Collapse in={!!estrategia.valorOperacaoFixo && !estrategia.percentualValorOperacao} sx={{ width: '100%' }}>
                            <Box sx={{ display: 'flex', alignItems: 'center' }}>
                                <FormControl fullWidth size="small" error={!!errors.tipoMoedaValorOperacao}>
                                    <InputLabel>Tipo de Moeda</InputLabel>
                                    <Select name="tipoMoedaValorOperacao" label="Tipo de Moeda" value={estrategia.tipoMoedaValorOperacao ?? ''} onChange={(e) => handleMainChange('tipoMoedaValorOperacao', e.target.value)}>
                                        {getTipoMoedaValorOperacaoOptions().map(option => ( <MenuItem key={option.value} value={option.value}> {option.label} </MenuItem> ))}
                                    </Select>
                                </FormControl>
                                <TooltipIcon description={tipoMoedaTooltip} />
                            </Box>
                        </Collapse>
                    </Grid>
                    <Grid item xs={12} sm={4}>
                        <TextField label="% do Saldo na Operação" type="number" size="small" fullWidth value={estrategia.percentualValorOperacao ?? ''} onChange={(e) => props.handlePercentChange('percentualValorOperacao', e.target.value, 100)} disabled={!!estrategia.valorOperacaoFixo} error={!!errors.percentualValorOperacao} helperText={errors.percentualValorOperacao || 'Sempre referente à moeda de cotação.'} />
                    </Grid>
                </Grid>
            </Box>
        </>
    );
};

export default Step3_RegrasCompra;