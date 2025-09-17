import type { PosicaoFaixasCompraVenda, TipoIndicador } from './enums';

export interface CondicaoCompraDTO {
    id?: string;
    clientId: number;
    tipoIndicador: TipoIndicador;
    valorIndicador?: number;
    posicaoFaixa?: PosicaoFaixasCompraVenda;
    valorOperacaoFixo?: number;
    percentualValorOperacao?: number;
}

export interface CondicaoVendaDTO {
    id?: string;
    clientId: number;
    tipoIndicador: TipoIndicador;
    valorIndicador?: number;
    posicaoFaixa?: PosicaoFaixasCompraVenda;
    quantiaSobreLucro?: number;
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
    condicoesCompra: CondicaoCompraDTO[];
    condicoesVenda: CondicaoVendaDTO[];
}

export type CriarEstrategiaDTO = Omit<Estrategia, 'id'>;