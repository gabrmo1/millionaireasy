import { PosicaoFaixasCompraVenda, TipoIndicador } from '../types/enums';
import { StatusOperacoes } from '../types/operacao';

// Mapeamento para PosicaoFaixasCompraVenda
export const posicaoFaixasLabels: Record<PosicaoFaixasCompraVenda, string> = {
    [PosicaoFaixasCompraVenda.ABAIXO]: 'Abaixo de',
    [PosicaoFaixasCompraVenda.ACIMA]: 'Acima de',
    [PosicaoFaixasCompraVenda.CRUZAR_PARA_BAIXO]: 'Cruzar para Baixo',
    [PosicaoFaixasCompraVenda.CRUZAR_PARA_CIMA]: 'Cruzar para Cima',
};

export const getPosicaoFaixasOptions = () => {
    return (Object.values(PosicaoFaixasCompraVenda) as PosicaoFaixasCompraVenda[]).map(value => ({
        value,
        label: posicaoFaixasLabels[value],
    }));
};

// NOVO: Mapeamento para TipoIndicador
export const tipoIndicadorLabels: Record<TipoIndicador, string> = {
    [TipoIndicador.RSI_CURTO]: 'RSI Curto',
    [TipoIndicador.RSI_MEDIO]: 'RSI Médio',
    [TipoIndicador.RSI_LONGO]: 'RSI Longo',
    [TipoIndicador.RSI_ESTOCASTICO_K]: 'RSI Estocástico %K',
    [TipoIndicador.RSI_ESTOCASTICO_D]: 'RSI Estocástico %D',
    [TipoIndicador.EMA]: 'EMA (Média Móvel Exponencial)',
    [TipoIndicador.SMA]: 'SMA (Média Móvel Simples)',
    [TipoIndicador.VOLUME]: 'Volume',
};

// NOVO: Função para obter as opções formatadas de TipoIndicador
export const getTipoIndicadorOptions = () => {
    return (Object.values(TipoIndicador) as TipoIndicador[]).map(value => ({
        value,
        label: tipoIndicadorLabels[value],
    }));
};


export const statusOperacoesLabels: Record<StatusOperacoes, string> = {
    [StatusOperacoes.EM_ANDAMENTO]: 'Em Andamento',
    [StatusOperacoes.PARADO]: 'Parado',
    [StatusOperacoes.FINALIZADO]: 'Finalizado',
};