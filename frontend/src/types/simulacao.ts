export interface CriarSimulacaoDTO {
    par: string;
    intervalo: string;
    idEstrategia: string;
    saldoInicial: number;
    dataInicio: string; // ISO 8601 UTC
    dataFim: string;   // ISO 8601 UTC
}

export interface Simulacao {
    id: string;
    par: string;
    intervalo: string;
    idEstrategia: string;
    saldoInicial: number;
    dataInicio: string;
    dataFim: string;
    status: 'AGUARDANDO' | 'EM_ANDAMENTO' | 'FINALIZADO' | 'ERRO';
    estrategia?: { nome: string };
}