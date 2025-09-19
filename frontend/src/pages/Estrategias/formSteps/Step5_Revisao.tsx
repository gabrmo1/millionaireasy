import React from 'react';
import { Box, Typography, List, ListItem, ListItemText } from '@mui/material';

interface Step5Props {
    estrategia: any;
}

const Step5_Revisao: React.FC<Step5Props> = ({ estrategia }) => {
    const valorOperacaoText = estrategia.valorOperacaoFixo
        ? `${estrategia.valorOperacaoFixo} (${estrategia.tipoMoedaValorOperacao})`
        : `${estrategia.percentualValorOperacao || 0}% do saldo`;

    const vendaPorLucroText = estrategia.vendaApenasPorLucro
        ? `Sim, com ${estrategia.percentualLucro || 0}% de lucro`
        : 'Não';

    const indicadoresAtivos = Object.entries(estrategia)
        .filter(([key, value]) => key.startsWith('utilizar') && value)
        .map(([key]) => key.replace('utilizar', ''))
        .join(', ');

    return (
        <Box>
            <Typography variant="h6" gutterBottom>Revise sua Estratégia</Typography>
            <List dense>
                <ListItem><ListItemText primary="Nome" secondary={estrategia.nome} /></ListItem>
                <ListItem><ListItemText primary="Condições de Compra" secondary={estrategia.condicoesCompra.length} /></ListItem>
                <ListItem><ListItemText primary="Condições de Venda" secondary={estrategia.condicoesVenda.length} /></ListItem>
                <ListItem><ListItemText primary="Valor da Operação" secondary={valorOperacaoText} /></ListItem>
                <ListItem><ListItemText primary="Venda por Lucro" secondary={vendaPorLucroText} /></ListItem>
                <ListItem><ListItemText primary="Indicadores Ativos" secondary={indicadoresAtivos} /></ListItem>
            </List>
        </Box>
    );
};

export default Step5_Revisao;