import React from 'react';
import { TextField, FormControl, InputLabel, Select, MenuItem, FormHelperText } from '@mui/material';
import Grid from '@mui/material/GridLegacy';

interface Step1Props {
    formData: { nome?: string; stablecoin?: string };
    handleFieldChange: (name: string, value: any) => void;
    errors: Record<string, string | null>;
    stablecoins: string[];
}

const Step1_InfoGerais: React.FC<Step1Props> = ({ formData, handleFieldChange, errors, stablecoins }) => {
    return (
        <Grid container spacing={2}>
            <Grid item xs={12} sm={6}>
                <TextField
                    name="nome"
                    label="Nome da Estratégia"
                    value={formData.nome || ''}
                    onChange={(e) => handleFieldChange('nome', e.target.value)}
                    error={!!errors.nome}
                    helperText={errors.nome || 'Dê um nome único e descritivo para sua estratégia.'}
                    fullWidth
                    required
                    size="small"
                    autoFocus
                />
            </Grid>
            <Grid item xs={12} sm={6}>
                <FormControl fullWidth size="small" error={!!errors.stablecoin} required>
                    <InputLabel>Stablecoin Padrão</InputLabel>
                    <Select
                        name="stablecoin"
                        label="Stablecoin Padrão"
                        value={formData.stablecoin ?? ''}
                        onChange={(e) => handleFieldChange('stablecoin', e.target.value)}
                    >
                        {stablecoins.map(option => (<MenuItem key={option} value={option}>{option}</MenuItem>))}
                    </Select>
                    <FormHelperText>{errors.stablecoin || 'Moeda base para operações de compra e lucro.'}</FormHelperText>
                </FormControl>
            </Grid>
        </Grid>
    );
};

export default Step1_InfoGerais;