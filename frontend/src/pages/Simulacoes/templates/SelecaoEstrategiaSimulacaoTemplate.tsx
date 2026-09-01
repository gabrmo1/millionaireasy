import React, { useState, useCallback } from 'react';
import { Box } from '@mui/material';
import type { TemplateProps } from '../../../components/common/forms/DynamicForm';
import EntitySelectorField from '../../../components/common/forms/EntitySelectorField';
import FormModal from '../../../components/common/modals/FormModal';
import EstrategiaForm from '../../Estrategias/EstrategiaForm';
import { getEstrategias } from '../../../services/estrategiaService';

const SelecaoEstrategiaSimulacaoTemplate: React.FC<TemplateProps> = ({ formData, errors, handleChange }) => {
    const [isEstrategiaModalOpen, setIsEstrategiaModalOpen] = useState(false);
    const [refreshKey, setRefreshKey] = useState(0);

    const handleOpenModal = useCallback(() => setIsEstrategiaModalOpen(true), []);
    const handleCloseModal = useCallback(() => setIsEstrategiaModalOpen(false), []);

    const handleEstrategiaCriada = useCallback(() => {
        setIsEstrategiaModalOpen(false);
        setRefreshKey(prev => prev + 1);
    }, []);

    return (
        <Box>
            <EntitySelectorField
                key={refreshKey}
                label="Estratégia"
                value={formData.idEstrategia || null}
                modalTitle="Selecione a Estratégia"
                fetcher={getEstrategias}
                displayAttribute="nome"
                onChange={(id) => handleChange('idEstrategia', id)}
                onCreateNew={handleOpenModal} // O motor agora renderiza o botão dentro do modal de seleção
                required
                error={!!errors.idEstrategia}
                helperText={errors.idEstrategia}
            />

            <FormModal open={isEstrategiaModalOpen} onClose={handleCloseModal} title="Cadastrar Nova Estratégia">
                <EstrategiaForm entityId={null} onClose={handleCloseModal} onSave={handleEstrategiaCriada} />
            </FormModal>
        </Box>
    );
};

export default React.memo(SelecaoEstrategiaSimulacaoTemplate);