import type { PosicaoFaixasCompraVenda, TipoIndicador } from './enums';

export interface CondicaoCompraDTO {
    id?: string;
    clientId: number;
    tipoIndicador: TipoIndicador;
    valorIndicador?: number;
    posicaoFaixa?: PosicaoFaixasCompraVenda;
}

export interface CondicaoVendaDTO {
    id?: string;
    clientId: number;
    tipoIndicador: TipoIndicador;
    valorIndicador?: number;
    posicaoFaixa?: PosicaoFaixasCompraVenda;
}

export interface Estrategia {
    id: string;
    nome: string;
    utilizarRsiCurto: boolean;
    periodoRsiCurto?: number;
    utilizarRsiMedio: boolean;
    periodoRsiMedio?: number;
    utilizarRsiLongo: boolean;
    periodoRsiLongo?: number;
    utilizarRsiEstocastico: boolean;
    periodoRsiEstocastico?: number;
    suavizacaoRsiEstocasticoD?: number;
    suavizacaoRsiEstocasticoK?: number;
    utilizarEma: boolean;
    periodoEma?: number;
    utilizarSma: boolean;
    periodoSma?: number;
    realizarLeituraVolume: boolean;
    valorOperacaoFixo?: number;
    percentualValorOperacao?: number;
    vendaApenasPorLucro?: boolean;
    percentualLucro?: number;
    condicoesCompra: CondicaoCompraDTO[];
    condicoesVenda: CondicaoVendaDTO[];
}

export type CriarEstrategiaDTO = Omit<Estrategia, 'id'>;