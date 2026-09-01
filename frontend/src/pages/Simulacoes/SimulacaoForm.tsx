import React, { useCallback, useEffect, useState } from 'react';
import dayjs from 'dayjs';
import DynamicForm from '../../components/common/forms/DynamicForm';
import { simulacaoFormMetadata } from './simulacaoFormMetadata';
import SelecaoParTemplate from '../Operacoes/templates/SelecaoParTemplate';
import SelecaoEstrategiaSimulacaoTemplate from './templates/SelecaoEstrategiaSimulacaoTemplate';
import { getEstrategias } from '../../services/estrategiaService';
import { createSimulacao } from '../../services/simulacaoService';
import type { Estrategia } from '../../types/estrategia';
import type { CriarSimulacaoDTO } from '../../types/simulacao';

interface SimulacaoFormProps {
    entityId: string | null;
    onClose: () => void;
    onSave: () => void;
}

const templates = {
    'SelecaoParTemplate': SelecaoParTemplate,
    'SelecaoEstrategiaSimulacaoTemplate': SelecaoEstrategiaSimulacaoTemplate
};

const SimulacaoForm: React.FC<SimulacaoFormProps> = ({ entityId, onClose, onSave }) => {
    const [estrategias, setEstrategias] = useState<Estrategia[]>([]);

    useEffect(() => {
        getEstrategias().then(setEstrategias).catch(console.error);
    }, []);

    const customValidator = useCallback((formData: Record<string, any>) => {
        const errors: Record<string, string | null> = {};
        const { dataInicio, dataFim, par, idEstrategia } = formData;

        if (dataFim && dayjs(dataFim).isAfter(dayjs())) {
            errors.dataFim = "A data final não pode ser no futuro.";
        }
        if (dataInicio && dataFim && dayjs(dataInicio).isAfter(dayjs(dataFim))) {
            errors.dataInicio = "A data inicial não pode ser maior que a data final.";
        }
        if (par && idEstrategia) {
            const strategy = estrategias.find(e => e.id === idEstrategia);
            if (strategy?.stablecoin && !par.endsWith(strategy.stablecoin)) {
                errors.idEstrategia = `Incompatível: A estratégia exige a stablecoin ${strategy.stablecoin}.`;
            }
        }
        return errors;
    }, [estrategias]);

    const handleSubmit = useCallback(async (formData: Record<string, any>) => {
        const payload: CriarSimulacaoDTO = {
            par: formData.par,
            intervalo: formData.intervalo,
            idEstrategia: formData.idEstrategia,
            saldoInicial: Number(formData.saldoInicial),
            dataInicio: dayjs(formData.dataInicio).toISOString(),
            dataFim: dayjs(formData.dataFim).toISOString(),
        };

        await createSimulacao(payload);
        onSave();
    }, [onSave]);

    return (
        <DynamicForm<any>
            metadata={simulacaoFormMetadata}
            entityId={entityId}
            onSubmit={handleSubmit}
            onClose={onClose}
            templates={templates}
            customValidator={customValidator}
            initialData={{ intervalo: '5m', saldoInicial: 1000 }}
        />
    );
};

export default React.memo(SimulacaoForm);