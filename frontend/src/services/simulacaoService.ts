import api from './api';
import type { CriarSimulacaoDTO, Simulacao } from '../types/simulacao';
import type { MonitoramentoDataDTO, RelatorioDesempenhoDTO } from '../types/monitoramento';

const BASE_URL = '/api/v1/simulacoes';

export const getSimulacoes = async (): Promise<Simulacao[]> => {
    const { data } = await api.get<Simulacao[]>(BASE_URL);
    return data;
};

export const createSimulacao = async (payload: CriarSimulacaoDTO): Promise<void> => {
    await api.post(BASE_URL, payload);
};

export const getSimulacaoMonitoramento = async (id: string): Promise<MonitoramentoDataDTO> => {
    const { data } = await api.get<MonitoramentoDataDTO>(`${BASE_URL}/${id}/monitoramento`);
    return data;
};

export const getSimulacaoRelatorioDesempenho = async (id: string, signal?: AbortSignal): Promise<RelatorioDesempenhoDTO> => {
    const { data } = await api.get<RelatorioDesempenhoDTO>(`${BASE_URL}/${id}/relatorio-desempenho`, { signal });
    return data;
};