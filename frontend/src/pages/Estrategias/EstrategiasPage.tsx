import React from 'react';
import GenericCrudPage from '../../components/common/pages/GenericCrudPage';
import { getEstrategias, deleteEstrategia } from '../../services/estrategiaService';
import { estrategiaGridColumns } from './estrategiaConfig';
import type { Estrategia } from '../../types/estrategia';

const EstrategiasPage: React.FC = () => (
    <GenericCrudPage<Estrategia>
        title="Estratégias"
        description="Gerencie suas estratégias de operação. Cada estratégia define os parâmetros de análise e as condições de compra e venda que o bot utilizará."
        fetcher={getEstrategias}
        deleter={deleteEstrategia}
        gridColumns={estrategiaGridColumns}
        createRoute="/estrategias/novo"
        editRoute="estrategias/editar"
    />
);

export default EstrategiasPage;