import React from 'react';
import { Box, IconButton, Paper, Typography, MenuItem, FormControl, InputLabel, Select, TextField, FormHelperText } from '@mui/material';
import Grid from '@mui/material/GridLegacy';
import DeleteIcon from '@mui/icons-material/Delete';
import type { CondicaoCompraDTO, CondicaoVendaDTO, Estrategia } from '../../types/estrategia';
import { TipoIndicador, OperadorLogico } from '../../types/enums';
import { getTipoIndicadorOptions, getPosicaoFaixasOptions } from '../../utils/enumMappings';
import { formatLeadingZeros } from "../../utils/inputFormatters.ts";

type Condicao = CondicaoCompraDTO | CondicaoVendaDTO;

interface CondicaoFormProps {
    condicao: Condicao;
    estrategia: Omit<Estrategia, 'id'>;
    index: number;
    totalCondicoes: number;
    onUpdate: (index: number, updatedCondicao: Condicao) => void;
    onRemove: (index: number) => void;
    title: string;
}

const CondicaoForm: React.FC<CondicaoFormProps> = ({ condicao, estrategia, index, totalCondicoes, onUpdate, onRemove, title }) => {

    const rsiIndicators = new Set<TipoIndicador>([
        TipoIndicador.RSI_CURTO,
        TipoIndicador.RSI_MEDIO,
        TipoIndicador.RSI_LONGO,
        TipoIndicador.RSI_ESTOCASTICO_K,
        TipoIndicador.RSI_ESTOCASTICO_D,
    ]);

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

    const handleChange = (name: string, value: any) => {
        onUpdate(index, { ...condicao, [name]: value });
    };

    const handleIndicatorValueChange = (value: string) => {
        const formattedValue = formatLeadingZeros(value);
        const isRsi = rsiIndicators.has(condicao.tipoIndicador);

        if (isRsi) {
            const numValue = Number(formattedValue);
            let finalValue: number | string = numValue;
            if (numValue < 0) finalValue = 0;
            if (numValue > 100) finalValue = 100;
            handleChange('valorIndicador', String(finalValue));
        } else {
            handleChange('valorIndicador', formattedValue);
        }
    };

    const tipoIndicadorOptions = getTipoIndicadorOptions();
    const posicaoFaixasOptions = getPosicaoFaixasOptions();
    const isLastCondition = index === totalCondicoes - 1;

    return (
        <>
            <Paper elevation={3} sx={{ p: 2, border: isCurrentIndicatorInvalid ? '1px solid red' : 'none' }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                    <Typography variant="h6">{title} #{index + 1}</Typography>
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
                                    value={condicao.valorIndicador ?? ''}
                                    onChange={(e) => handleIndicatorValueChange(e.target.value)}
                                />
                            </Grid>
                        </>
                    )}
                </Grid>
            </Paper>

            {!isLastCondition && (
                <Box sx={{ display: 'flex', justifyContent: 'center', my: -1.5, zIndex: 2, position: 'relative' }}>
                    <FormControl size="small" sx={{ minWidth: 80 }}>
                        <Select
                            value={condicao.operadorParaProxima || 'AND'}
                            onChange={(e) => handleChange('operadorParaProxima', e.target.value as OperadorLogico)}
                            sx={{
                                borderRadius: '50px',
                                '& .MuiSelect-select': {
                                    py: 0.5,
                                    px: 2,
                                    fontWeight: 'bold',
                                    backgroundColor: (theme) => theme.palette.background.paper,
                                },
                            }}
                        >
                            <MenuItem value={'AND'}>E</MenuItem>
                            <MenuItem value={'OR'}>OU</MenuItem>
                        </Select>
                    </FormControl>
                </Box>
            )}
        </>
    );
};

export default CondicaoForm;