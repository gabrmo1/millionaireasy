import { PosicaoFaixasCompraVenda } from '../types/enums';
import { StatusOperacoes } from '../types/operacao';

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

export const statusOperacoesLabels: Record<StatusOperacoes, string> = {
    [StatusOperacoes.EM_ANDAMENTO]: 'Em Andamento',
    [StatusOperacoes.PARADO]: 'Parado',
    [StatusOperacoes.FINALIZADO]: 'Finalizado',
};