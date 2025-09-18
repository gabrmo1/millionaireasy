import React from 'react';
import { Box, IconButton, Paper, Typography, MenuItem, FormControl, InputLabel, Select, TextField, FormHelperText } from '@mui/material';
import Grid from '@mui/material/GridLegacy';
import DeleteIcon from '@mui/icons-material/Delete';
import type { CondicaoVendaDTO, Estrategia } from '../../types/estrategia';
import { TipoIndicador, PosicaoFaixasCompraVenda } from '../../types/enums';
import { getTipoIndicadorOptions, getPosicaoFaixasOptions } from '../../utils/enumMappings';

interface CondicaoVendaFormProps {
    condicao: CondicaoVendaDTO;
    estrategia: Omit<Estrategia, 'id'>;
    index: number;
    onUpdate: (index: number, updatedCondicao: CondicaoVendaDTO) => void;
    onRemove: (index: number) => void;
}

const CondicaoVendaForm: React.FC<CondicaoVendaFormProps> = ({ condicao, estrategia, index, onUpdate, onRemove }) => {

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

    const isCurrentIndicatorInvalid = condicao.tipoIndicador && !isIndicatorEnabled(condicao.tipoIndicador);

    const handleChange = (name: string, value: string | number) => {
        onUpdate(index, { ...condicao, [name]: value });
    };

    const handlePercentChange = (name: string, value: string) => {
        const numValue = Number(value);
        if (numValue < 0) {
            onUpdate(index, { ...condicao, [name]: 0 });
        } else if (numValue > 100) {
            onUpdate(index, { ...condicao, [name]: 100 });
        } else {
            onUpdate(index, { ...condicao, [name]: numValue });
        }
    };

    const tipoIndicadorOptions = getTipoIndicadorOptions();
    const posicaoFaixasOptions = getPosicaoFaixasOptions();

    return (
        <Paper elevation={3} sx={{ p: 2, mb: 2, border: isCurrentIndicatorInvalid ? '1px solid red' : 'none' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6">Condição de Venda #{index + 1}</Typography>
                <IconButton onClick={() => onRemove(index)} color="error">
                    <DeleteIcon />
                </IconButton>
            </Box>
            <Grid container spacing={2}>
                <Grid item xs={12}>
                    <FormControl fullWidth size="small">
                        <InputLabel>Indicador</InputLabel>
                        <Select
                            value={condicao.tipoIndicador || ''}
                            label="Indicador"
                            onChange={(e) => handleChange('tipoIndicador', e.target.value)}
                            error={isCurrentIndicatorInvalid}
                        >
                            {tipoIndicadorOptions.map(opt => (
                                <MenuItem key={opt.value} value={opt.value} disabled={!isIndicatorEnabled(opt.value)}>
                                    {opt.label}
                                </MenuItem>
                            ))}
                        </Select>
                        {isCurrentIndicatorInvalid && (
                            <FormHelperText error>
                                Este indicador não está ativo na configuração da estratégia.
                            </FormHelperText>
                        )}
                    </FormControl>
                </Grid>
                {condicao.tipoIndicador && (
                    <>
                        <Grid item xs={6}>
                            <FormControl fullWidth size="small">
                                <InputLabel>Posição</InputLabel>
                                <Select
                                    value={condicao.posicaoFaixa || ''}
                                    label="Posição"
                                    onChange={(e) => handleChange('posicaoFaixa', e.target.value)}
                                >
                                    {posicaoFaixasOptions.map(opt => <MenuItem key={opt.value} value={opt.value}>{opt.label}</MenuItem>)}
                                </Select>
                            </FormControl>
                        </Grid>
                        <Grid item xs={6}>
                            <TextField
                                label="Valor do Indicador"
                                type="number"
                                size="small"
                                fullWidth
                                value={condicao.valorIndicador || ''}
                                onChange={(e) => handleChange('valorIndicador', e.target.value)}
                            />
                        </Grid>
                    </>
                )}
                <Grid item xs={12}>
                    <TextField
                        label="Vender com Lucro de (%)"
                        type="number"
                        size="small"
                        fullWidth
                        value={condicao.quantiaSobreLucro || ''}
                        onChange={(e) => handlePercentChange('quantiaSobreLucro', e.target.value)}
                    />
                </Grid>
            </Grid>
        </Paper>
    );
};

export default CondicaoVendaForm;