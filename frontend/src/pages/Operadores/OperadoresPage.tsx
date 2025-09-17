import React from 'react';
import GenericCrudPage from '../../components/common/pages/GenericCrudPage';
import { operadorGridColumns } from './operadorConfig';
import { getOperadores, deleteOperador } from '../../services/operadorService';
import type {Operador} from '../../types/operador';

const OperadoresPage: React.FC = () => {
    return (
        <GenericCrudPage<Operador>
            title="Operadores"
            description="Gerencie os operadores (chaves de API) que o sistema utilizará para se conectar à corretora e executar as ordens."
            fetcher={getOperadores}
            deleter={deleteOperador}
            gridColumns={operadorGridColumns}
            createRoute="/operadores/novo"
            editRoute="operadores/editar"
        />
    );
};

export default OperadoresPage;