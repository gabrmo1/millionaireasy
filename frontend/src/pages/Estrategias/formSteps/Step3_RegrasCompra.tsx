import React, { useRef, useEffect } from 'react';
import { Box, Button, Typography, Divider, TextField, FormControl, InputLabel, Select, MenuItem, Alert, FormHelperText } from '@mui/material';
import Grid from '@mui/material/GridLegacy';
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import CondicaoForm from '../CondicaoForm';

interface Step3Props {
    estrategia: any;
    errors: Record<string, string | null>;
    stablecoins: string[];
    addCondicaoCompra: () => void;
    updateCondicaoCompra: (index: number, updated: any) => void;
    removeCondicaoCompra: (index: number) => void;
    handleValorOperacaoChange: (name: 'valorOperacaoFixo' | 'percentualValorOperacao', value: string | undefined) => void;
    handlePercentChange: (name: 'percentualValorOperacao' | 'percentualLucro', value: string, max: number) => void;
    handleMainChange: (name: string, value: any) => void;
}

const Step3_RegrasCompra: React.FC<Step3Props> = (props) => {
    const {
        estrategia, errors, stablecoins, addCondicaoCompra, updateCondicaoCompra,
        removeCondicaoCompra, handleValorOperacaoChange, handleMainChange
    } = props;

    const lastConditionRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (lastConditionRef.current) {
            lastConditionRef.current.scrollIntoView({ behavior: 'smooth', block: 'end' });
        }
    }, [estrategia.condicoesCompra.length]);

    const hasValorPreenchido = (estrategia.valorOperacaoFixo && Number(estrategia.valorOperacaoFixo) > 0) ||
        (estrategia.percentualValorOperacao && Number(estrategia.percentualValorOperacao) > 0);

    return (
        <>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6">Condições de Compra</Typography>
                <Button startIcon={<AddCircleOutlineIcon />} onClick={addCondicaoCompra}>Adicionar</Button>
            </Box>
            {estrategia.condicoesCompra.map((condicao: any, index: number) => (
                <div key={condicao.clientId} ref={index === estrategia.condicoesCompra.length - 1 ? lastConditionRef : null}>
                    <CondicaoForm
                        title="Condição de Compra"
                        index={index}
                        condicao={condicao}
                        estrategia={estrategia}
                        totalCondicoes={estrategia.condicoesCompra.length}
                        onUpdate={updateCondicaoCompra}
                        onRemove={removeCondicaoCompra}
                    />
                </div>
            ))}
            {estrategia.condicoesCompra.length === 0 && <Alert severity="info">Nenhuma condição de compra adicionada.</Alert>}
            <Divider sx={{ my: 3 }} />
            <Box>
                <Typography variant="h6" sx={{ mb: 2 }}>Parâmetros de Compra</Typography>
                <Grid container spacing={2} alignItems="flex-start">
                    <Grid item xs={12} sm={6}>
                        <TextField label="Valor Fixo Operação" type="number" size="small" fullWidth value={estrategia.valorOperacaoFixo ?? ''} onChange={(e) => handleValorOperacaoChange('valorOperacaoFixo', e.target.value)} disabled={!!estrategia.percentualValorOperacao} inputProps={{ min: 0 }} error={!!errors.valorOperacaoFixo} helperText={errors.valorOperacaoFixo || ' '} />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                        <TextField label="% do Saldo na Operação" type="number" size="small" fullWidth value={estrategia.percentualValorOperacao ?? ''} onChange={(e) => props.handlePercentChange('percentualValorOperacao', e.target.value, 100)} disabled={!!estrategia.valorOperacaoFixo} error={!!errors.percentualValorOperacao} helperText={errors.percentualValorOperacao || 'Sempre referente à moeda de cotação.'} />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                        <FormControl fullWidth size="small" error={!!errors.stablecoin} required={hasValorPreenchido}>
                            <InputLabel>Stablecoin</InputLabel>
                            <Select name="stablecoin" label="Stablecoin" value={estrategia.stablecoin ?? ''} onChange={(e) => handleMainChange('stablecoin', e.target.value)}>
                                {stablecoins.map(option => ( <MenuItem key={option} value={option}> {option} </MenuItem> ))}
                            </Select>
                            <FormHelperText>{errors.stablecoin || 'Obrigatório se um valor de operação for definido.'}</FormHelperText>
                        </FormControl>
                    </Grid>
                </Grid>
            </Box>
        </>
    );
};

export default Step3_RegrasCompra;