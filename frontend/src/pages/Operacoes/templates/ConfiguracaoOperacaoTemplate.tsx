import React from 'react';
import { FormControl, InputLabel, Select, MenuItem, Typography, FormHelperText } from '@mui/material';
import Grid from '@mui/material/GridLegacy';
import type { TemplateProps } from '../../../components/common/forms/DynamicForm';
import EntitySelectorField from '../../../components/common/forms/EntitySelectorField';
import { getOperadores } from "../../../services/operadorService.ts";
import { getEstrategias } from "../../../services/estrategiaService.ts";

const ConfiguracaoOperacaoTemplate: React.FC<TemplateProps> = ({ formData, errors, handleChange }) => {
    const intervalos = ["5m", "15m", "30m", "1h", "4h", "1d"];

    return (
        <>
            <Typography variant="h6" gutterBottom sx={{ mb: 2 }}>Configure a Operação</Typography>
            <Grid container spacing={3}>
                <Grid item xs={12}>
                    <FormControl fullWidth required error={!!errors.intervalo}>
                        <InputLabel>Intervalo do Gráfico</InputLabel>
                        <Select
                            name="intervalo"
                            label="Intervalo do Gráfico"
                            value={formData.intervalo ?? '15m'}
                            onChange={(e) => handleChange('intervalo', e.target.value)}
                            size="small"
                        >
                            {intervalos.map(i => <MenuItem key={i} value={i}>{i}</MenuItem>)}
                        </Select>
                        <FormHelperText>{errors.intervalo}</FormHelperText>
                    </FormControl>
                </Grid>
                <Grid item xs={12}>
                    <EntitySelectorField
                        label="Operador (Chaves API)"
                        value={formData.idOperador || null}
                        modalTitle="Selecione um Operador"
                        fetcher={getOperadores}
                        displayAttribute="nome"
                        onChange={(id) => handleChange('idOperador', id)}
                        required
                        error={!!errors.idOperador}
                        helperText={errors.idOperador}
                        size="small"
                    />
                </Grid>
                <Grid item xs={12}>
                    <EntitySelectorField
                        label="Estratégia de Negociação"
                        value={formData.idEstrategia || null}
                        modalTitle="Selecione uma Estratégia"
                        fetcher={getEstrategias}
                        displayAttribute="nome"
                        onChange={(id) => handleChange('idEstrategia', id)}
                        required
                        error={!!errors.idEstrategia}
                        helperText={errors.idEstrategia}
                        size="small"
                    />
                </Grid>
            </Grid>
        </>
    );
};

export default ConfiguracaoOperacaoTemplate;