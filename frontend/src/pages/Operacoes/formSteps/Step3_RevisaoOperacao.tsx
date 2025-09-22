import React from 'react';
import { Box, Typography, List, ListItem, ListItemText } from '@mui/material';
import type { Estrategia } from '../../../types/estrategia';

interface Step3Props {
    formData: {
        par?: string;
        intervalo?: string;
        idOperador?: string;
        idEstrategia?: string;
    };
    selectedEstrategia: Estrategia | null;
}

const Step3_RevisaoOperacao: React.FC<Step3Props> = ({ formData, selectedEstrategia }) => {
    return (
        <Box>
            <Typography variant="h6" gutterBottom>Revise sua Operação</Typography>
            <List dense>
                <ListItem>
                    <ListItemText primary="Par de Moedas" secondary={formData.par || 'Não definido'} />
                </ListItem>
                <ListItem>
                    <ListItemText primary="Intervalo" secondary={formData.intervalo || 'Não definido'} />
                </ListItem>
                <ListItem>
                    <ListItemText primary="Estratégia" secondary={selectedEstrategia?.nome || 'Não definida'} />
                </ListItem>
            </List>
        </Box>
    );
};

export default Step3_RevisaoOperacao;