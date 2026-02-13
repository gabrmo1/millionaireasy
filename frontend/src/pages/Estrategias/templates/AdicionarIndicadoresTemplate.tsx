import React from 'react';
import type { TemplateProps } from '../../../components/common/forms/DynamicForm';
import Step2_Indicadores from '../formSteps/Step2_Indicadores';
import type { IndicadorConfigUI } from '../../../types/estrategia';
import { generateIndicatorAlias } from '../estrategiaUtils';

const AdicionarIndicadoresTemplate: React.FC<TemplateProps> = ({ formData, errors, handleChange }) => {

    const indicadores = (formData.indicadoresConfig || []) as IndicadorConfigUI[];

    const updateIndicadores = (newIndicadores: IndicadorConfigUI[]) => {
        handleChange('indicadoresConfig', newIndicadores);
    };

    const addIndicador = () => {
        const newIndicador: IndicadorConfigUI = {
            clientId: Math.random(),
            alias: '',
            tipoIndicador: 'RSI_CURTO',
            parametros: { periodoRsiCurto: 7 }
        };
        newIndicador.alias = generateIndicatorAlias(newIndicador);
        updateIndicadores([...indicadores, newIndicador]);
    };

    const updateIndicador = (index: number, updated: IndicadorConfigUI) => {
        const newIndicadores = [...indicadores];
        updated.alias = generateIndicatorAlias(updated);
        newIndicadores[index] = updated;
        updateIndicadores(newIndicadores);
    };

    const removeIndicador = (index: number) => {
        const aliasToRemove = indicadores[index].alias;
        const newIndicadores = indicadores.filter((_: any, i: number) => i !== index);

        // Também remove condições que usam o indicador
        const condicoesCompra = (formData.condicoesCompra || []).filter((c: any) => c.operandoAReferencia !== aliasToRemove && c.operandoBReferencia !== aliasToRemove);
        const condicoesVenda = (formData.condicoesVenda || []).filter((c: any) => c.operandoAReferencia !== aliasToRemove && c.operandoBReferencia !== aliasToRemove);

        handleChange('indicadoresConfig', newIndicadores);
        handleChange('condicoesCompra', condicoesCompra);
        handleChange('condicoesVenda', condicoesVenda);
    };

    return (
        <Step2_Indicadores
            indicadores={indicadores}
            onAdd={addIndicador}
            onRemove={removeIndicador}
            onUpdate={updateIndicador}
            errors={errors}
        />
    );
};

export default AdicionarIndicadoresTemplate;