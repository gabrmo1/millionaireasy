import React from 'react';
import {
    Box,
    Button,
    Typography,
    IconButton,
    Paper,
    TextField,
    MenuItem,
    FormControl,
    InputLabel,
    Select, FormHelperText, Alert
} from '@mui/material';
import Grid from '@mui/material/GridLegacy';
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import DeleteIcon from '@mui/icons-material/Delete';
import type { IndicadorConfigUI } from '../../../types/estrategia';
import { getTipoIndicadorOptions } from '../../../utils/enumMappings';
import { indicadorParamsConfig } from '../indicadorParamsConfig';
import { formatLeadingZeros } from "../../../utils/inputFormatters";

interface Step2Props {
    indicadores: IndicadorConfigUI[];
    onAdd: () => void;
    onRemove: (index: number) => void;
    onUpdate: (index: number, updated: IndicadorConfigUI) => void;
    errors: Record<string, string | null>;
}

const Step2_Indicadores: React.FC<Step2Props> = ({ indicadores, onAdd, onRemove, onUpdate, errors }) => {

    const handleParamChange = (index: number, paramKey: string, value: string) => {
        let processedValue = value.replace(/[^0-9]/g, '');
        processedValue = formatLeadingZeros(processedValue);
        const updated = { ...indicadores[index] };
        updated.parametros = { ...updated.parametros, [paramKey]: Number(processedValue) || 0 };
        onUpdate(index, updated);
    };

    const handleTypeChange = (index: number, value: any) => {
        const tipo = value as IndicadorConfigUI['tipoIndicador'];
        const paramsConf = indicadorParamsConfig[tipo];
        const newParams: { [key: string]: number } = {};

        if (paramsConf) {
            paramsConf.forEach(p => {
                newParams[p.key] = p.defaultValue;
            });
        }

        const updated = { ...indicadores[index], tipoIndicador: tipo, parametros: newParams };
        onUpdate(index, updated);
    }

    return (
        <Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6">Indicadores</Typography>
                <Button startIcon={<AddCircleOutlineIcon />} onClick={onAdd}>Adicionar</Button>
            </Box>

            {errors.indicadores && <Alert severity="error" sx={{ mb: 2 }}>{errors.indicadores}</Alert>}

            {indicadores.map((indicador, index) => (
                <Paper key={indicador.clientId} elevation={2} sx={{ p: 2, mb: 2 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                        <Typography variant="subtitle1" fontWeight="bold">Indicador #{index + 1}</Typography>
                        <IconButton onClick={() => onRemove(index)} color="error"><DeleteIcon /></IconButton>
                    </Box>
                    <Grid container spacing={2}>
                        <Grid item xs={12}>
                            <FormControl fullWidth size="small" error={!!errors[`indicador_${index}_alias`]}>
                                <InputLabel>Tipo de Indicador</InputLabel>
                                <Select
                                    label="Tipo de Indicador"
                                    value={indicador.tipoIndicador}
                                    onChange={(e) => handleTypeChange(index, e.target.value)}
                                >
                                    {getTipoIndicadorOptions().map(opt => <MenuItem key={opt.value} value={opt.value}>{opt.label}</MenuItem>)}
                                </Select>
                                {errors[`indicador_${index}_alias`] && <FormHelperText>{errors[`indicador_${index}_alias`]}</FormHelperText>}
                            </FormControl>
                        </Grid>
                        {indicadorParamsConfig[indicador.tipoIndicador]?.map(param => (
                            <Grid item xs={6} sm={4} key={param.key}>
                                <TextField
                                    label={param.label}
                                    type="number"
                                    fullWidth
                                    size="small"
                                    value={indicador.parametros[param.key] || ''}
                                    onChange={(e) => handleParamChange(index, param.key, e.target.value)}
                                    inputProps={{ min: param.min, max: param.max }}
                                    error={!!errors[`indicador_${index}_param_${param.key}`]}
                                    helperText={errors[`indicador_${index}_param_${param.key}`] || ' '}
                                />
                            </Grid>
                        ))}
                    </Grid>
                </Paper>
            ))}
        </Box>
    );
};

export default Step2_Indicadores;