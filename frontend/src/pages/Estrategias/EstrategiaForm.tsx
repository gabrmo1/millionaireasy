import React, { useCallback } from 'react';
import DynamicForm from '../../components/common/forms/DynamicForm';
import type { CriarEstrategiaDTO, Estrategia, IndicadorConfigDTO } from '../../types/estrategia';
import { getEstrategiaById, createEstrategia, updateEstrategia } from '../../services/estrategiaService';
import { estrategiaFormMetadata } from './estrategiaFormMetadata';
import { indicadorParamsConfig } from './indicadorParamsConfig';

import AdicionarIndicadoresTemplate from './templates/AdicionarIndicadoresTemplate';
import AdicionarRegrasCompraTemplate from './templates/AdicionarRegrasCompraTemplate';
import AdicionarRegrasVendaTemplate from './templates/AdicionarRegrasVendaTemplate';


interface EstrategiaFormProps {
    entityId: string | null;
    onClose: () => void;
    onSave: () => void;
}

const templates = {
    'AdicionarIndicadoresTemplate': AdicionarIndicadoresTemplate,
    'AdicionarRegrasCompraTemplate': AdicionarRegrasCompraTemplate,
    'AdicionarRegrasVendaTemplate': AdicionarRegrasVendaTemplate,
};

const initialData = {
    indicadoresConfig: [],
    condicoesCompra: [],
    condicoesVenda: [],
    vendaApenasPorLucro: false,
    percentualLucro: 10,
    stablecoin: "USDT"
};

const EstrategiaForm: React.FC<EstrategiaFormProps> = ({ entityId, onClose, onSave }) => {

    const estrategiaCustomValidator = (formData: Record<string, any>): Record<string, string | null> => {
        const errors: Record<string, string | null> = {};
        const indicadores: IndicadorConfigDTO[] = formData.indicadoresConfig || [];

        if (indicadores.length === 0) {
            errors.indicadores = 'É necessário configurar pelo menos um indicador.';
        }
        indicadores.forEach((indicador, index) => {
            const paramsConfig = indicadorParamsConfig[indicador.tipoIndicador];
            if (!paramsConfig) return;

            paramsConfig.forEach(param => {
                const value = indicador.parametros[param.key];
                const errorKey = `indicador_${index}_param_${param.key}`;

                if (param.required && (value === undefined || value === null || value === 0)) {
                    errors[errorKey] = 'Obrigatório.';
                } else if (value !== undefined && value !== null) {
                    if (param.min !== undefined && Number(value) < param.min) {
                        errors[errorKey] = `Mínimo ${param.min}.`;
                    }
                    if (param.max !== undefined && Number(value) > param.max) {
                        errors[errorKey] = `Máximo ${param.max}.`;
                    }
                }
            });
        });

        if (formData.vendaApenasPorLucro) {
            const percentualLucro = formData.percentualLucro;
            if (percentualLucro === undefined || percentualLucro === null || percentualLucro === '') {
                errors.percentualLucro = 'Obrigatório com Take Profit.';
            } else if (Number(percentualLucro) < 0.01) {
                errors.percentualLucro = 'Deve ser no mínimo 0.01.';
            }
        }

        return errors;
    };

    const handleSubmit = useCallback(async (formData: Record<string, any>) => {
        const payload = { ...formData } as CriarEstrategiaDTO;
        if (payload.valorOperacaoFixo) payload.valorOperacaoFixo = Number(payload.valorOperacaoFixo);
        if (payload.percentualValorOperacao) payload.percentualValorOperacao = Number(payload.percentualValorOperacao);
        if (payload.percentualLucro) payload.percentualLucro = Number(payload.percentualLucro);

        await createEstrategia(payload);
        onSave();
    }, [onSave]);

    const handleUpdate = useCallback(async (id: string, formData: Record<string, any>) => {
        const payload = { ...formData } as CriarEstrategiaDTO;
        if (payload.valorOperacaoFixo) payload.valorOperacaoFixo = Number(payload.valorOperacaoFixo);
        if (payload.percentualValorOperacao) payload.percentualValorOperacao = Number(payload.percentualValorOperacao);
        if (payload.percentualLucro) payload.percentualLucro = Number(payload.percentualLucro);

        await updateEstrategia(id, payload);
        onSave();
    }, [onSave]);

    return (
        <DynamicForm<Estrategia>
            metadata={estrategiaFormMetadata}
            entityId={entityId}
            fetcher={getEstrategiaById}
            onSubmit={handleSubmit}
            onUpdate={handleUpdate}
            onClose={onClose}
            templates={templates}
            customValidator={estrategiaCustomValidator}
            initialData={initialData}
        />
    );
};

export default EstrategiaForm;