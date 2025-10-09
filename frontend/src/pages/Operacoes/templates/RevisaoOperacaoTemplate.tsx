import React, { useState, useEffect } from 'react';
import { Box, Typography, List, ListItem, ListItemText, CircularProgress } from '@mui/material';
import type { Estrategia } from '../../../types/estrategia';
import type { TemplateProps } from '../../../components/common/forms/DynamicForm';
import { getEstrategiaById } from "../../../services/estrategiaService.ts";

const RevisaoOperacaoTemplate: React.FC<TemplateProps> = ({ formData }) => {
    const [selectedEstrategia, setSelectedEstrategia] = useState<Estrategia | null>(null);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (formData.idEstrategia) {
            setLoading(true);
            getEstrategiaById(formData.idEstrategia)
                .then(setSelectedEstrategia)
                .catch(err => console.error("Falha ao carregar estratégia para revisão:", err))
                .finally(() => setLoading(false));
        } else {
            setSelectedEstrategia(null);
        }
    }, [formData.idEstrategia]);

    return (
        <Box>
            <Typography variant="h6" gutterBottom>Revise sua Operação</Typography>
            {loading ? <CircularProgress /> : (
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
            )}
        </Box>
    );
};

export default RevisaoOperacaoTemplate;