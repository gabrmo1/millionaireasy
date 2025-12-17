import api from './api';
import type { CriarOperacaoDTO, Operacao } from '../types/operacao';

const BASE_URL = '/v1/operacoes';

export const getOperacoes = async (): Promise<Operacao[]> => {
    const response = await api.get<Operacao[]>(BASE_URL);
    return response.data;
};

export const getOperacaoById = async (id: string): Promise<Operacao> => {
    const response = await api.get<Operacao>(`${BASE_URL}/${id}`);
    return response.data;
};

export const createOperacao = async (data: CriarOperacaoDTO): Promise<Operacao> => {
    const response = await api.post<Operacao>(BASE_URL, data);
    return response.data;
};

export const updateOperacao = async (id: string, data: CriarOperacaoDTO): Promise<Operacao> => {
    const response = await api.put<Operacao>(`${BASE_URL}/${id}`, data);
    return response.data;
};

export const deleteOperacao = async (id: string): Promise<void> => {
    await api.delete(`${BASE_URL}/${id}`);
};

export const iniciarOperacao = async (id: string): Promise<void> => {
    await api.post(`${BASE_URL}/${id}/start`);
};

export const pararOperacao = async (id: string): Promise<void> => {
    await api.post(`${BASE_URL}/${id}/stop`);
};