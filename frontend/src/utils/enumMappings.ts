import { PosicaoFaixasCompraVenda, TipoIndicador, TipoMoedaValorOperacao } from '../types/enums';
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

// Mapeamento para TipoIndicador
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

export const getTipoIndicadorOptions = () => {
    return (Object.values(TipoIndicador) as TipoIndicador[]).map(value => ({
        value,
        label: tipoIndicadorLabels[value],
    }));
};

// Mapeamento para StatusOperacoes
export const statusOperacoesLabels: Record<StatusOperacoes, string> = {
    [StatusOperacoes.EM_ANDAMENTO]: 'Em Andamento',
    [StatusOperacoes.PARADO]: 'Parado',
    [StatusOperacoes.FINALIZADO]: 'Finalizado',
};

// NOVO: Mapeamento para TipoMoedaValorOperacao
export const tipoMoedaValorOperacaoLabels: Record<TipoMoedaValorOperacao, string> = {
    [TipoMoedaValorOperacao.BASE]: 'Moeda Base (Ex: BTC em BTC/USDT)',
    [TipoMoedaValorOperacao.QUOTE]: 'Moeda de Cotação (Ex: USDT em BTC/USDT)',
};

export const getTipoMoedaValorOperacaoOptions = () => {
    return (Object.values(TipoMoedaValorOperacao) as TipoMoedaValorOperacao[]).map(value => ({
        value,
        label: tipoMoedaValorOperacaoLabels[value],
    }));
};