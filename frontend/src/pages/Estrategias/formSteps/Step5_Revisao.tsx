import React from 'react';
import { Typography, List, ListItem, ListItemText, Paper, Divider } from '@mui/material';
import type { CriarEstrategiaDTO } from '../../../types/estrategia';

// Definindo o tipo esperado para formData
type FormDataProps = Omit<CriarEstrategiaDTO, 'valorOperacaoFixo' | 'percentualValorOperacao' | 'percentualLucro'> & {
    valorOperacaoFixo?: string;
    percentualValorOperacao?: string;
    percentualLucro?: string;
};

interface Step5Props {
    formData: Partial<FormDataProps>;
}

const Step5_Revisao: React.FC<Step5Props> = ({ formData }) => {
    const valorCompra = formData.valorOperacaoFixo ? `${formData.valorOperacaoFixo} ${formData.stablecoin}`
        : formData.percentualValorOperacao ? `${formData.percentualValorOperacao}% do saldo em ${formData.stablecoin}`
            : 'Não definido';

    const takeProfit = formData.vendaApenasPorLucro ? `Sim, em ${formData.percentualLucro || 0}% de lucro` : 'Não';

    return (
        <Paper variant="outlined" sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>Revise sua Estratégia</Typography>
            <List dense>
                <ListItem><ListItemText primary="Nome da Estratégia" secondary={formData.nome} /></ListItem>
                <ListItem><ListItemText primary="Stablecoin Padrão" secondary={formData.stablecoin} /></ListItem>
                <Divider component="li" sx={{ my: 1 }} />
                <ListItem><ListItemText primary="Indicadores Configurados" secondary={formData.indicadoresConfig?.length || 0} /></ListItem>
                <Divider component="li" sx={{ my: 1 }} />
                <ListItem><ListItemText primary="Valor por Compra" secondary={valorCompra} /></ListItem>
                <ListItem><ListItemText primary="Condições de Compra" secondary={formData.condicoesCompra?.length || 0} /></ListItem>
                <Divider component="li" sx={{ my: 1 }} />
                <ListItem><ListItemText primary="Venda por Take Profit" secondary={takeProfit} /></ListItem>
                <ListItem><ListItemText primary="Condições de Venda" secondary={formData.condicoesVenda?.length || 0} /></ListItem>
            </List>
            <Typography variant="body2" sx={{ mt: 2 }}>
                Se todas as informações estiverem corretas, clique em "Salvar" para finalizar a criação da sua estratégia.
            </Typography>
        </Paper>
    );
};

export default Step5_Revisao;