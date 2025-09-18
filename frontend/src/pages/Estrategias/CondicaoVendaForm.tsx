import React from 'react';
import { Box, IconButton, Paper, Typography, MenuItem, FormControl, InputLabel, Select, TextField } from '@mui/material';
import Grid from '@mui/material/GridLegacy';
import DeleteIcon from '@mui/icons-material/Delete';
import type { CondicaoVendaDTO } from '../../types/estrategia';
import { TipoIndicador, PosicaoFaixasCompraVenda } from '../../types/enums';

interface CondicaoVendaFormProps {
    condicao: CondicaoVendaDTO;
    index: number;
    onUpdate: (index: number, updatedCondicao: CondicaoVendaDTO) => void;
    onRemove: (index: number) => void;
}

const CondicaoVendaForm: React.FC<CondicaoVendaFormProps> = ({ condicao, index, onUpdate, onRemove }) => {

    const handleChange = (name: string, value: any) => {
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

    return (
        <Paper elevation={3} sx={{ p: 2, mb: 2 }}>
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
                        >
                            {Object.values(TipoIndicador).map(ind => <MenuItem key={ind} value={ind}>{ind}</MenuItem>)}
                        </Select>
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
                                    {Object.values(PosicaoFaixasCompraVenda).map(pos => <MenuItem key={pos} value={pos}>{pos}</MenuItem>)}
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