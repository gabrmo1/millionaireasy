import React from 'react';
import { Box, Button, Typography, Alert } from '@mui/material';
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import CondicaoForm from '../CondicaoForm';
import type { CondicaoDTO, IndicadorConfigDTO } from '../../../types/estrategia';

interface Step4Props {
    indicadores: IndicadorConfigDTO[];
    condicoes: CondicaoDTO[];
    onAdd: () => void;
    onRemove: (index: number) => void;
    onUpdate: (index: number, updated: CondicaoDTO) => void;
    errors: Record<string, string | null>;
}

const Step4_RegrasVenda: React.FC<Step4Props> = (props) => {
    const { indicadores, condicoes, onAdd, onRemove, onUpdate, errors } = props;

    const isLastCondition = (index: number) => index === condicoes.length - 1;

    return (
        <Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6">Condições de Venda (Opcional)</Typography>
                <Button startIcon={<AddCircleOutlineIcon />} onClick={onAdd} disabled={indicadores.length === 0}>Adicionar</Button>
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