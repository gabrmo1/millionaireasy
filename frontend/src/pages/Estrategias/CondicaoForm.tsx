import React from 'react';
import { Box, IconButton, Paper, Typography, MenuItem, FormControl, InputLabel, Select, TextField } from '@mui/material';
import Grid from '@mui/material/GridLegacy';
import DeleteIcon from '@mui/icons-material/Delete';
import { OperadorLogico } from '../../types/enums';
import type { CondicaoDTO, IndicadorConfigDTO } from '../../types/estrategia';
import { getTipoOperandoOptions, getOperadorComparacaoOptions } from '../../utils/enumMappings';

interface CondicaoFormProps {
    condicao: CondicaoDTO;
    index: number;
    totalCondicoes: number;
    indicadores: IndicadorConfigDTO[];
    onUpdate: (index: number, updatedCondicao: CondicaoDTO) => void;
    onRemove: (index: number) => void;
    tipoCondicao: 'Compra' | 'Venda';
    errors: Record<string, string | null>;
}

const CondicaoForm: React.FC<CondicaoFormProps> = ({ condicao, index, totalCondicoes, indicadores, onUpdate, onRemove, tipoCondicao, errors }) => {
    const handleChange = (field: keyof CondicaoDTO, value: any) => {
        onUpdate(index, { ...condicao, [field]: value });
    };

    const renderOperandoInput = (lado: 'A' | 'B') => {
        const tipoKey = lado === 'A' ? 'operandoATipo' : 'operandoBTipo';
        const refKey = lado === 'A' ? 'operandoAReferencia' : 'operandoBReferencia';
        const valorKey = lado === 'A' ? 'operandoAValor' : 'operandoBValor';
        const errorKey = `condicao_${tipoCondicao.toLowerCase()}_${index}_valor_${lado.toLowerCase()}`;

        const inputComponent = () => {
            switch (condicao[tipoKey]) {
                case 'INDICADOR':
                    return (
                        <FormControl fullWidth size="small">
                            <InputLabel>Indicador</InputLabel>
                            <Select
                                value={condicao[refKey] || ''}
                                label="Indicador"
                                onChange={(e) => handleChange(refKey, e.target.value)}
                            >
                                {indicadores.map(i => <MenuItem key={i.alias} value={i.alias}>{i.alias}</MenuItem>)}
                            </Select>
                        </FormControl>
                    );
                case 'VALOR_FIXO':
                    return (
                        <TextField
                            label="Valor"
                            type="number"
                            size="small"
                            fullWidth
                            value={condicao[valorKey] ?? ''}
                            onChange={(e) => handleChange(valorKey, e.target.value === '' ? undefined : parseFloat(e.target.value))}
                            error={!!errors[errorKey]}
                            helperText={errors[errorKey] || ' '}
                        />
                    );
                case 'PRECO_FECHAMENTO':
                default:
                    return null;
            }
        };

        return condicao[tipoKey] !== 'PRECO_FECHAMENTO' ? <Box mt={1.5}>{inputComponent()}</Box> : null;
    };

    const isLastCondition = index === totalCondicoes - 1;

    return (
        <>
            <Paper elevation={3} sx={{ p: 2 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                    <Typography variant="h6">Condição de {tipoCondicao} #{index + 1}</Typography>
                    <IconButton onClick={() => onRemove(index)} color="error"><DeleteIcon /></IconButton>
                </Box>
                <Grid container spacing={2} alignItems="flex-start">
                    <Grid item xs={12} sm={5}>
                        <FormControl fullWidth size="small">
                            <InputLabel>Operando A</InputLabel>
                            <Select
                                value={condicao.operandoATipo || ''}
                                label="Operando A"
                                onChange={(e) => handleChange('operandoATipo', e.target.value)}
                            >
                                {getTipoOperandoOptions().map(opt => <MenuItem key={opt.value} value={opt.value}>{opt.label}</MenuItem>)}
                            </Select>
                        </FormControl>
                        {renderOperandoInput('A')}
                    </Grid>
                    <Grid item xs={12} sm={2} sx={{ pt: { xs: 2, sm: '16px !important' } }}>
                        <FormControl fullWidth size="small">
                            <InputLabel>Operador</InputLabel>
                            <Select
                                value={condicao.operador || ''}
                                label="Operador"
                                onChange={(e) => handleChange('operador', e.target.value)}
                            >
                                {getOperadorComparacaoOptions().map(opt => <MenuItem key={opt.value} value={opt.value}>{opt.label}</MenuItem>)}
                            </Select>
                        </FormControl>
                    </Grid>
                    <Grid item xs={12} sm={5}>
                        <FormControl fullWidth size="small">
                            <InputLabel>Operando B</InputLabel>
                            <Select
                                value={condicao.operandoBTipo || ''}
                                label="Operando B"
                                onChange={(e) => handleChange('operandoBTipo', e.target.value)}
                            >
                                {getTipoOperandoOptions().map(opt => <MenuItem key={opt.value} value={opt.value}>{opt.label}</MenuItem>)}
                            </Select>
                        </FormControl>
                        {renderOperandoInput('B')}
                    </Grid>
                </Grid>
            </Paper>

            {!isLastCondition && (
                <Box sx={{ display: 'flex', justifyContent: 'center', my: 1, zIndex: 2, position: 'relative' }}>
                    <FormControl size="small" sx={{ minWidth: 80 }}>
                        <Select
                            value={condicao.operadorParaProxima || 'AND'}
                            onChange={(e) => handleChange('operadorParaProxima', e.target.value as OperadorLogico)}
                            sx={{ borderRadius: '50px', '& .MuiSelect-select': { py: 0.5, px: 2, fontWeight: 'bold', backgroundColor: (theme) => theme.palette.background.paper, }, }}
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