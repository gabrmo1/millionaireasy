import type { TipoIndicador, OperadorLogico, TipoOperando, OperadorComparacao } from './enums';

export interface IndicadorConfigDTO {
    id?: string;
    alias: string;
    tipoIndicador: TipoIndicador;
    parametros: {
        [key: string]: number;
    };
}

export interface CondicaoDTO {
    id?: string;
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

export interface CriarEstrategiaRequestDTO {
    nome: string;
    valorOperacaoFixo?: number;
    stablecoin: string;
    percentualValorOperacao?: number;
    vendaApenasPorLucro?: boolean;
    percentualLucro?: number;
    indicadoresConfig: IndicadorConfigDTO[];
    condicoesCompra: CondicaoCompraDTO[];
    condicoesVenda: CondicaoVendaDTO[];
}

export interface IndicadorConfigUI extends IndicadorConfigDTO {
    clientId: number;
}

export interface CondicaoUI extends CondicaoDTO {
    clientId: number;
}

export interface Estrategia {
    id: string;
    nome: string;
    valorOperacaoFixo?: number;
    stablecoin?: string;
    percentualValorOperacao?: number;
    vendaApenasPorLucro?: boolean;
    percentualLucro?: number;
    indicadoresConfig: IndicadorConfigUI[];
    condicoesCompra: CondicaoUI[];
    condicoesVenda: CondicaoUI[];
}

export type EstrategiaFormData = Omit<Estrategia, 'id'>;