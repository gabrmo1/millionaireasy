import React from 'react';
import { Box, IconButton, Paper, Typography, MenuItem, FormControl, InputLabel, Select, TextField } from '@mui/material';
import Grid from '@mui/material/GridLegacy';
import DeleteIcon from '@mui/icons-material/Delete';
import type { CondicaoCompraDTO } from '../../types/estrategia';
import { TipoIndicador, PosicaoFaixasCompraVenda } from '../../types/enums';

interface CondicaoCompraFormProps {
    condicao: CondicaoCompraDTO;
    index: number;
    onUpdate: (index: number, updatedCondicao: CondicaoCompraDTO) => void;
    onRemove: (index: number) => void;
}

const CondicaoCompraForm: React.FC<CondicaoCompraFormProps> = ({ condicao, index, onUpdate, onRemove }) => {

    const handleChange = (name: string, value: any) => {
        const updatedCondicao = { ...condicao, [name]: value };
        onUpdate(index, updatedCondicao);
    };

    return (
        <Paper elevation={3} sx={{ p: 2, mb: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6">Condição de Compra #{index + 1}</Typography>
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
            </Grid>
        </Paper>
    );
};

export default CondicaoCompraForm;