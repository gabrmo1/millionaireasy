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
    operador: Operador;
    estrategia: Estrategia;
}

export type CriarOperacaoDTO = Omit<Operacao, 'id' | 'status' | 'dataCriacao' | 'operador' | 'estrategia'> & {
    idOperador: string;
    idEstrategia: string;
};