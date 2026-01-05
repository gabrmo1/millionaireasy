import type { Operador } from './operador';
import type { Estrategia } from './estrategia';

export const StatusOperacoes = {
    EM_ANDAMENTO: 'EM_ANDAMENTO',
    PARADO: 'PARADO',
    FINALIZADO: 'FINALIZADO'
} as const;

export type StatusOperacoes = typeof StatusOperacoes[keyof typeof StatusOperacoes];

export interface Operacao {
    id: string;
    status: StatusOperacoes;
    dataCriacao: string;
    dataInicio: string;
    dataFim: string;
    par: string;
    intervalo: string;
    operador?: Operador;
    estrategia: Estrategia;
    modoTeste: boolean;
    saldoInicial?: number;
}

export interface Compra {
    id: string;
    dataCompra: string;
    valorOperacao: number;
    valorMoeda: number;
    volume: number;
    snapshotIndicadores: string;
}

export interface Venda {
    id: string;
    dataVenda: string;
    valorCompra: number;
    valorVenda: number;
    lucro: number;
    snapshotIndicadores: string;
}

export interface HistoricoOperacao {
    compras: Compra[];
    vendas: Venda[];
}

export type CriarOperacaoDTO = Omit<Operacao, 'id' | 'status' | 'dataCriacao' | 'operador' | 'estrategia'> & {
    idOperador?: string;
    idEstrategia: string;
};