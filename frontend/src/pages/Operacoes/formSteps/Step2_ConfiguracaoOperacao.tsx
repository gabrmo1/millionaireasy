import React from 'react';
import { FormControl, InputLabel, Select, MenuItem, FormHelperText, Typography } from '@mui/material';
import Grid from '@mui/material/GridLegacy';
import EntitySelectorField from '../../../components/common/forms/EntitySelectorField';
import { getOperadores } from "../../../services/operadorService.ts";
import { getEstrategias } from "../../../services/estrategiaService.ts";

interface Step2Props {
    formData: { intervalo?: string; idOperador?: string; idEstrategia?: string };
    errors: Record<string, string | null>;
    onFieldChange: (name: string, value: any) => void;
    onEstrategiaChange: (id: string | null) => void;
}

const Step2_ConfiguracaoOperacao: React.FC<Step2Props> = ({ formData, errors, onFieldChange, onEstrategiaChange }) => {
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
                            value={formData.intervalo ?? ''}
                            onChange={(e) => onFieldChange('intervalo', e.target.value)}
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
                        onChange={(id) => onFieldChange('idOperador', id)}
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
                        onChange={onEstrategiaChange}
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

export default Step2_ConfiguracaoOperacao;