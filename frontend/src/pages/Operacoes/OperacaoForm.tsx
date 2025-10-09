import React, { useCallback, useState, useEffect } from 'react';
import { createOperacao, updateOperacao, getOperacaoById } from "../../services/operacaoService";
import { getEstrategias } from "../../services/estrategiaService";
import { getStablecoinPairs } from "../../services/mexcService";
import type { CriarOperacaoDTO, Operacao } from '../../types/operacao';
import type { Estrategia } from '../../types/estrategia';
import type { SymbolInfo } from '../../types/mexc';
import DynamicForm from '../../components/common/forms/DynamicForm';
import { operacaoFormMetadata } from './operacaoFormMetadata';

import SelecaoParTemplate from './templates/SelecaoParTemplate';
import ConfiguracaoOperacaoTemplate from './templates/ConfiguracaoOperacaoTemplate';
import RevisaoOperacaoTemplate from './templates/RevisaoOperacaoTemplate';

interface OperacaoFormProps {
    entityId: string | null;
    onClose: () => void;
    onSave: () => void;
}

const templates = {
    'SelecaoParTemplate': SelecaoParTemplate,
    'ConfiguracaoOperacaoTemplate': ConfiguracaoOperacaoTemplate,
    'RevisaoOperacaoTemplate': RevisaoOperacaoTemplate,
};

const initialData = { intervalo: '15m' };

const OperacaoForm: React.FC<OperacaoFormProps> = ({ entityId, onClose, onSave }) => {
    const [allPairs, setAllPairs] = useState<SymbolInfo[]>([]);
    const [allStrategies, setAllStrategies] = useState<Estrategia[]>([]);

    useEffect(() => {
        getStablecoinPairs().then(setAllPairs).catch(err => console.error("Falha ao buscar pares.", err));
        getEstrategias().then(setAllStrategies).catch(err => console.error("Falha ao buscar estratégias.", err));
    }, []);

    const customValidator = (formData: Record<string, any>): Record<string, string | null> => {
        const errors: Record<string, string | null> = {};
        const { par, idEstrategia } = formData;

        if (par && idEstrategia) {
            const selectedPair = allPairs.find(p => p.symbol === par);
            const selectedStrategy = allStrategies.find(e => e.id === idEstrategia);

            if (selectedPair && selectedStrategy && selectedStrategy.stablecoin) {
                if (selectedPair.quoteAsset !== selectedStrategy.stablecoin) {
                    errors.idEstrategia = `Incompatível: O par opera com ${selectedPair.quoteAsset}, mas a estratégia usa ${selectedStrategy.stablecoin}.`;
                }
            }
        }

        return errors;
    };

    const handleSubmit = useCallback(async (formData: Record<string, any>) => {
        const payload = { ...formData } as CriarOperacaoDTO;
        await createOperacao(payload);
        onSave();
    }, [onSave]);

    const handleUpdate = useCallback(async (id: string, formData: Record<string, any>) => {
        const payload = { ...formData } as CriarOperacaoDTO;
        await updateOperacao(id, payload);
        onSave();
    }, [onSave]);

    return (
        <DynamicForm<Operacao>
            metadata={operacaoFormMetadata}
            entityId={entityId}
            fetcher={getOperacaoById}
            onSubmit={handleSubmit}
            onUpdate={handleUpdate}
            onClose={onClose}
            templates={templates}
            customValidator={customValidator}
            initialData={initialData}
        />
    );
};

export default OperacaoForm;