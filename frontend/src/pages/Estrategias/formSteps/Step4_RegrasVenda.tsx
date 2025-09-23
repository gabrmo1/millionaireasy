import React from 'react';
import { Box, Button, Typography, Alert, FormControlLabel, Checkbox, Collapse, TextField, Divider } from '@mui/material';
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import CondicaoForm from '../CondicaoForm';
import type { CondicaoDTO, IndicadorConfigDTO } from '../../../types/estrategia';

interface FormDataProps {
    vendaApenasPorLucro?: boolean;
    percentualLucro?: string;
}

interface Step4Props {
    formData: FormDataProps;
    indicadores: IndicadorConfigDTO[];
    condicoes: CondicaoDTO[];
    onAdd: () => void;
    onRemove: (index: number) => void;
    onUpdate: (index: number, updated: CondicaoDTO) => void;
    handleFieldChange: (name: string, value: any) => void;
    errors: Record<string, string | null>;
}

const Step4_RegrasVenda: React.FC<Step4Props> = (props) => {
    const { formData, indicadores, condicoes, onAdd, onRemove, onUpdate, handleFieldChange, errors } = props;

    const isLastCondition = (index: number) => index === condicoes.length - 1;

    return (
        <Box>
            <Typography variant="h6" gutterBottom>Parâmetros de Venda</Typography>
            <FormControlLabel control={<Checkbox checked={!!formData.vendaApenasPorLucro} onChange={(e) => handleFieldChange('vendaApenasPorLucro', e.target.checked)} name="vendaApenasPorLucro" size="small" />} label="Venda por Take Profit (Lucro)" />
            <Collapse in={!!formData.vendaApenasPorLucro} timeout="auto" unmountOnExit>
                <Box sx={{ pl: 2, pt: 1.5, ml: 1.5, mt: 1 }}>
                    <TextField label="% de Lucro para Venda" type="number" size="small" fullWidth value={formData.percentualLucro ?? ''} onChange={(e) => handleFieldChange('percentualLucro', e.target.value)} sx={{ maxWidth: '300px' }} error={!!errors.percentualLucro} helperText={errors.percentualLucro || ' '} />
                </Box>
            </Collapse>

            <Divider sx={{ my: 2 }} />

            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6">Condições de Venda (Opcional)</Typography>
                <Button startIcon={<AddCircleOutlineIcon />} onClick={onAdd} disabled={indicadores.length === 0}>Adicionar Condição</Button>
            </Box>
            {indicadores.length === 0 && <Alert severity="warning" sx={{ mb: 2 }}>Você deve configurar ao menos um indicador na Etapa 2 para criar regras.</Alert>}

            {condicoes.map((condicao, index) => (
                <Box key={condicao.clientId} mb={isLastCondition(index) ? 0 : 2}>
                    <CondicaoForm
                        tipoCondicao="Venda"
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

            {condicoes.length === 0 && <Alert severity="info">Nenhuma condição de venda por indicador adicionada.</Alert>}
        </Box>
    );
};

export default Step4_RegrasVenda;