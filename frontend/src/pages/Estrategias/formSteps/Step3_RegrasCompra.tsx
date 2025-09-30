import React from 'react';
import { Box, Button, Typography, Alert, TextField, Divider, InputAdornment } from '@mui/material';
import Grid from '@mui/material/GridLegacy';
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import CondicaoForm from '../CondicaoForm';
import type { CondicaoDTO, IndicadorConfigDTO } from '../../../types/estrategia';

interface FormDataProps {
    valorOperacaoFixo?: string;
    percentualValorOperacao?: string;
    stablecoin?: string;
}

interface Step3Props {
    formData: FormDataProps;
    indicadores: IndicadorConfigDTO[];
    condicoes: CondicaoDTO[];
    onAdd: () => void;
    onRemove: (index: number) => void;
    onUpdate: (index: number, updated: CondicaoDTO) => void;
    handleFieldChange: (name: string, value: any) => void;
    errors: Record<string, string | null>;
}

const Step3_RegrasCompra: React.FC<Step3Props> = (props) => {
    const { formData, indicadores, condicoes, onAdd, onRemove, onUpdate, handleFieldChange, errors } = props;

    const isLastCondition = (index: number) => index === condicoes.length - 1;

    return (
        <Box>
            <Typography variant="h6" gutterBottom>Investimento</Typography>
            <Grid container spacing={2} sx={{ mb: 3 }}>
                <Grid item xs={12} sm={6}>
                    <TextField
                        label="Valor Fixo por Operação"
                        type="number"
                        size="small"
                        fullWidth
                        value={formData.valorOperacaoFixo ?? ''}
                        onChange={(e) => handleFieldChange('valorOperacaoFixo', e.target.value)}
                        disabled={!!formData.percentualValorOperacao}
                        inputProps={{ min: 0 }}
                        error={!!errors.valorOperacaoFixo}
                        helperText={errors.valorOperacaoFixo || ' '}
                        InputProps={{
                            endAdornment: (
                                <InputAdornment position="end">
                                    {formData.stablecoin}
                                </InputAdornment>
                            ),
                        }}
                    />
                </Grid>
                <Grid item xs={12} sm={6}>
                    <TextField
                        label="% do Saldo por Operação"
                        type="number"
                        size="small"
                        fullWidth
                        value={formData.percentualValorOperacao ?? ''}
                        onChange={(e) => handleFieldChange('percentualValorOperacao', e.target.value)}
                        disabled={!!formData.valorOperacaoFixo}
                        error={!!errors.percentualValorOperacao}
                        helperText={errors.percentualValorOperacao || `Baseado na Stablecoin (${formData.stablecoin})`}
                        inputProps={{ min: 0, max: 100 }}
                        InputProps={{
                            endAdornment: (
                                <InputAdornment position="end">
                                    % {formData.stablecoin}
                                </InputAdornment>
                            )
                        }}
                    />
                </Grid>
            </Grid>

            <Divider sx={{ my: 2 }} />

            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6">Configurações de compra</Typography>
                <Button startIcon={<AddCircleOutlineIcon />} onClick={onAdd} disabled={indicadores.length === 0}>Adicionar</Button>
            </Box>
            {indicadores.length === 0 && <Alert severity="warning" sx={{ mb: 2 }}>Você deve configurar ao menos um indicador na etapa anterior para criar regras.</Alert>}

            {condicoes.map((condicao, index) => (
                <Box key={condicao.clientId} mb={isLastCondition(index) ? 0 : 2}>
                    <CondicaoForm
                        tipoCondicao="Compra"
                        index={index}
                        condicao={condicao}
                        indicadores={indicadores}
                        totalCondicoes={condicoes.length}
                        onUpdate={(idx, updated) => onUpdate(idx, updated)}
                        onRemove={onRemove}
                        errors={errors}
                    />
                </Box>
            ))}

            {condicoes.length === 0 && <Alert severity="info">Nenhuma condição de compra adicionada. A compra será acionada se os parâmetros acima forem definidos.</Alert>}
        </Box>
    );
};

export default Step3_RegrasCompra;