import React from 'react';
import type { TemplateProps } from '../../../components/common/forms/DynamicForm';
import Step4_RegrasVenda from '../formSteps/Step4_RegrasVenda';
import type { CondicaoDTO } from '../../../types/estrategia';
import { TipoOperando, OperadorComparacao } from '../../../types/enums';

const AdicionarRegrasVendaTemplate: React.FC<TemplateProps> = ({ formData, errors, handleChange }) => {

    const condicoes = formData.condicoesVenda || [];
    const indicadores = formData.indicadoresConfig || [];

    const updateCondicoes = (newCondicoes: CondicaoDTO[]) => {
        handleChange('condicoesVenda', newCondicoes);
    };

    const addCondicao = () => {
        const newCondicao: CondicaoDTO = {
            clientId: Math.random(),
            ordem: condicoes.length,
            operadorParaProxima: 'AND',
            operandoATipo: TipoOperando.INDICADOR,
            operador: OperadorComparacao.MENOR_QUE,
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
        <Step4_RegrasVenda
            // formData={formData}
            indicadores={indicadores}
            condicoes={condicoes}
            onAdd={addCondicao}
            onRemove={removeCondicao}
            onUpdate={updateCondicao}
            // handleFieldChange={handleChange}
            errors={errors}
        />
    );
};

export default AdicionarRegrasVendaTemplate;