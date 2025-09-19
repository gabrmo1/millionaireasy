import React from 'react';
import { Box, Button, Typography, Divider, FormControlLabel, Checkbox, Collapse, TextField, Alert } from '@mui/material';
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import CondicaoVendaForm from '../CondicaoVendaForm';

interface Step4Props {
    estrategia: any;
    handleMainChange: (name: string, value: any) => void;
    handlePercentChange: (name: 'percentualValorOperacao' | 'percentualLucro', value: string, max: number) => void;
    addCondicaoVenda: () => void;
    updateCondicaoVenda: (index: number, updated: any) => void;
    removeCondicaoVenda: (index: number) => void;
}

const Step4_RegrasVenda: React.FC<Step4Props> = (props) => {
    const {
        estrategia, handleMainChange, handlePercentChange, addCondicaoVenda,
        updateCondicaoVenda, removeCondicaoVenda
    } = props;

    return (
        <>
            <Box>
                <Typography variant="h6" sx={{ mb: 2 }}>Parâmetros de Venda</Typography>
                <FormControlLabel control={<Checkbox checked={!!estrategia.vendaApenasPorLucro} onChange={(e) => handleMainChange('vendaApenasPorLucro', e.target.checked)} name="vendaApenasPorLucro" size="small" />} label="Efetuar venda somente sobre % de lucro" />
                <Collapse in={!!estrategia.vendaApenasPorLucro} timeout="auto" unmountOnExit>
                    <Box sx={{ pl: 2, pt: 1.5, ml: 1.5, mt: 1 }}>
                        <TextField label="% de Lucro para Venda" type="number" size="small" fullWidth value={estrategia.percentualLucro ?? ''} onChange={(e) => handlePercentChange('percentualLucro', e.target.value, 9999)} sx={{ maxWidth: '300px' }} />
                    </Box>
                </Collapse>
            </Box>
            <Divider sx={{ my: 3 }} />
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6">Condições de Venda (Opcional)</Typography>
                <Button startIcon={<AddCircleOutlineIcon />} onClick={addCondicaoVenda}>Adicionar</Button>
            </Box>
            {estrategia.condicoesVenda.map((condicao: any, index: number) => (
                <CondicaoVendaForm
                    key={condicao.clientId}
                    index={index}
                    condicao={condicao}
                    estrategia={estrategia}
                    onUpdate={updateCondicaoVenda}
                    onRemove={removeCondicaoVenda}
                />
            ))}
            {estrategia.condicoesVenda.length === 0 && <Alert severity="info">Nenhuma condição de venda por indicador adicionada.</Alert>}
        </>
    );
};

export default Step4_RegrasVenda;