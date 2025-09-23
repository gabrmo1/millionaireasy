import type { TipoIndicador, OperadorLogico, TipoOperando, OperadorComparacao } from './enums';

export interface IndicadorConfigDTO {
    id?: string;
    clientId: number; // Apenas para controle no frontend
    alias: string;
    tipoIndicador: TipoIndicador;
    parametros: {
        [key: string]: number;
    };
}

export interface CondicaoDTO {
    id?: string;
    clientId: number; // Apenas para controle no frontend
    ordem: number;
    operadorParaProxima?: OperadorLogico;
    operandoATipo: TipoOperando;
    operandoAReferencia?: string;
    operandoAValor?: number;
    operador: OperadorComparacao;
    operandoBTipo: TipoOperando;
    operandoBReferencia?: string;
    operandoBValor?: number;
}

export type CondicaoCompraDTO = CondicaoDTO;
export type CondicaoVendaDTO = CondicaoDTO;

export interface Estrategia {
    id: string;
    nome: string;
    valorOperacaoFixo?: number;
    stablecoin?: string;
    percentualValorOperacao?: number;
    vendaApenasPorLucro?: boolean;
    percentualLucro?: number;
    indicadoresConfig: IndicadorConfigDTO[];
    condicoesCompra: CondicaoCompraDTO[];
    condicoesVenda: CondicaoVendaDTO[];
}

export type CriarEstrategiaDTO = Omit<Estrategia, 'id'>;