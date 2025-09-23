import { PosicaoFaixasCompraVenda, TipoIndicador, OperadorComparacao, TipoOperando } from '../types/enums';
import { StatusOperacoes } from '../types/operacao';

// Mapeamento para PosicaoFaixasCompraVenda (SERÁ REMOVIDO POSTERIORMENTE)
export const posicaoFaixasLabels: Record<PosicaoFaixasCompraVenda, string> = {
    [PosicaoFaixasCompraVenda.ABAIXO]: 'Abaixo de',
    [PosicaoFaixasCompraVenda.ACIMA]: 'Acima de',
    [PosicaoFaixasCompraVenda.CRUZAR_PARA_BAIXO]: 'Cruzar para Baixo',
    [PosicaoFaixasCompraVenda.CRUZAR_PARA_CIMA]: 'Cruzar para Cima',
};
export const getPosicaoFaixasOptions = () => (Object.values(PosicaoFaixasCompraVenda) as PosicaoFaixasCompraVenda[]).map(value => ({ value, label: posicaoFaixasLabels[value] }));

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
export const getTipoIndicadorOptions = () => (Object.values(TipoIndicador) as TipoIndicador[]).map(value => ({ value, label: tipoIndicadorLabels[value] }));

// Mapeamento para StatusOperacoes
export const statusOperacoesLabels: Record<StatusOperacoes, string> = {
    [StatusOperacoes.EM_ANDAMENTO]: 'Em Andamento',
    [StatusOperacoes.PARADO]: 'Parado',
    [StatusOperacoes.FINALIZADO]: 'Finalizado',
};

export const tipoOperandoLabels: Record<TipoOperando, string> = {
    [TipoOperando.INDICADOR]: 'Indicador',
    [TipoOperando.PRECO_FECHAMENTO]: 'Preço de Fechamento',
    [TipoOperando.VALOR_FIXO]: 'Valor Fixo',
};
export const getTipoOperandoOptions = () => (Object.values(TipoOperando) as TipoOperando[]).map(value => ({ value, label: tipoOperandoLabels[value] }));

export const operadorComparacaoLabels: Record<OperadorComparacao, string> = {
    [OperadorComparacao.CRUZOU_PARA_CIMA]: 'Cruzou para Cima',
    [OperadorComparacao.CRUZOU_PARA_BAIXO]: 'Cruzou para Baixo',
    [OperadorComparacao.MAIOR_QUE]: 'Maior que',
    [OperadorComparacao.MENOR_QUE]: 'Menor que',
};
export const getOperadorComparacaoOptions = () => (Object.values(OperadorComparacao) as OperadorComparacao[]).map(value => ({ value, label: operadorComparacaoLabels[value] }));