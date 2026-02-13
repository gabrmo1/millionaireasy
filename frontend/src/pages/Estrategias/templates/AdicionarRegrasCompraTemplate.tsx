import React from 'react';
import type { TemplateProps } from '../../../components/common/forms/DynamicForm';
import Step3_RegrasCompra from '../formSteps/Step3_RegrasCompra';
import type { CondicaoUI, IndicadorConfigUI } from '../../../types/estrategia';
import { TipoOperando, OperadorComparacao } from '../../../types/enums';

const AdicionarRegrasCompraTemplate: React.FC<TemplateProps> = ({ formData, errors, handleChange }) => {

    const condicoes = (formData.condicoesCompra || []) as CondicaoUI[];
    const indicadores = (formData.indicadoresConfig || []) as IndicadorConfigUI[];

    const updateCondicoes = (newCondicoes: CondicaoUI[]) => {
        handleChange('condicoesCompra', newCondicoes);
    };

    const addCondicao = () => {
        const newCondicao: CondicaoUI = {
            clientId: Math.random(),
            ordem: condicoes.length,
            operadorParaProxima: 'AND',
            operandoATipo: TipoOperando.INDICADOR,
            operador: OperadorComparacao.MAIOR_QUE,
            operandoBTipo: TipoOperando.VALOR_FIXO,
        };
        updateCondicoes([...condicoes, newCondicao]);
    };

    const updateCondicao = (index: number, updated: CondicaoUI) => {
        const newCondicoes = [...condicoes];
        newCondicoes[index] = updated;
        updateCondicoes(newCondicoes);
    };

    const removeCondicao = (index: number) => {
        const newCondicoes = condicoes.filter((_: any, i: number) => i !== index)
            .map((cond: CondicaoUI, newIndex: number) => ({ ...cond, ordem: newIndex }));
        updateCondicoes(newCondicoes);
    };

    return (
        <Step3_RegrasCompra
            indicadores={indicadores}
            condicoes={condicoes}
            onAdd={addCondicao}
            onRemove={removeCondicao}
            onUpdate={updateCondicao}
            errors={errors}
        />
    );
};

export default AdicionarRegrasCompraTemplate;