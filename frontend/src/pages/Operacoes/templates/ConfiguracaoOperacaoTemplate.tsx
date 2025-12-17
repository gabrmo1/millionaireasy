import React from 'react';
import {
    FormControl,
    InputLabel,
    Select,
    MenuItem,
    Typography,
    FormHelperText,
    FormControlLabel,
    Switch,
    TextField
} from '@mui/material';
import Grid from '@mui/material/GridLegacy';
import type { TemplateProps } from '../../../components/common/forms/DynamicForm';
import EntitySelectorField from '../../../components/common/forms/EntitySelectorField';
import { getOperadores } from "../../../services/operadorService.ts";
import { getEstrategias } from "../../../services/estrategiaService.ts";

const ConfiguracaoOperacaoTemplate: React.FC<TemplateProps> = ({ formData, errors, handleChange }) => {
    const intervalos = ["5m", "15m", "30m", "1h", "4h", "1d"];

    const handleModoTesteChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const isTeste = e.target.checked;
        handleChange('modoTeste', isTeste);

        if (isTeste) {
            handleChange('idOperador', null);
        } else {
            handleChange('saldoInicial', null);
        }
    };

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

                {/* Renderização Condicional: Saldo (Teste) ou Operador (Real) */}
                {formData.modoTeste ? (
                    <Grid item xs={12}>
                        <TextField
                            fullWidth
                            label="Saldo Inicial (USDT Fictício)"
                            type="number"
                            value={formData.saldoInicial || ''}
                            onChange={(e) => handleChange('saldoInicial', Number(e.target.value))}
                            required
                            error={!!errors.saldoInicial}
                            helperText={errors.saldoInicial || "Valor utilizado para simular compras e vendas"}
                            size="small"
                            InputProps={{ inputProps: { min: 0 } }}
                        />
                    </Grid>
                ) : (
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
                )}

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

                {/* Switch de Modo Teste */}
                <Grid item xs={12}>
                    <FormControlLabel
                        control={
                            <Switch
                                checked={!!formData.modoTeste}
                                onChange={handleModoTesteChange}
                                name="modoTeste"
                                color="primary"
                            />
                        }
                        label={formData.modoTeste ? "Modo Simulação (Teste)" : "Modo Real (Produção)"}
                    />
                    <Typography variant="caption" display="block" color="text.secondary">
                        {formData.modoTeste
                            ? "Operações simuladas não executam ordens na exchange. Requer saldo fictício."
                            : "Operações reais utilizam suas chaves API para executar ordens na MEXC."}
                    </Typography>
                </Grid>

            </Grid>
        </>
    );
};

export default ConfiguracaoOperacaoTemplate;