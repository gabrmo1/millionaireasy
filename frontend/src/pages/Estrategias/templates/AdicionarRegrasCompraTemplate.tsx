import React from 'react';
import type { TemplateProps } from '../../../components/common/forms/DynamicForm';
import Step3_RegrasCompra from '../formSteps/Step3_RegrasCompra';
import type { CondicaoDTO } from '../../../types/estrategia';
import { TipoOperando, OperadorComparacao } from '../../../types/enums';

const AdicionarRegrasCompraTemplate: React.FC<TemplateProps> = ({ formData, errors, handleChange }) => {

    const condicoes = formData.condicoesCompra || [];
    const indicadores = formData.indicadoresConfig || [];

    const updateCondicoes = (newCondicoes: CondicaoDTO[]) => {
        handleChange('condicoesCompra', newCondicoes);
    };

    const addCondicao = () => {
        const newCondicao: CondicaoDTO = {
            clientId: Math.random(),
            ordem: condicoes.length,
            operadorParaProxima: 'AND',
            operandoATipo: TipoOperando.INDICADOR,
            operador: OperadorComparacao.MAIOR_QUE,
            operandoBTipo: TipoOperando.VALOR_FIXO,
        };
        updateCondicoes([...condicoes, newCondicao]);
    };

    const updateCondicao = (index: number, updated: CondicaoDTO) => {
        const newCondicoes = [...condicoes];
        newCondicoes[index] = updated;
        updateCondicoes(newCondicoes);
    };

    const removeCondicao = (index: number) => {
        const newCondicoes = condicoes.filter((_: any, i: number) => i !== index)
            .map((cond: CondicaoDTO, newIndex: number) => ({ ...cond, ordem: newIndex }));
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