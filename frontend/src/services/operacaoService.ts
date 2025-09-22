import api from './api';
import type {Operacao, CriarOperacaoDTO} from '../types/operacao';

export const getOperacoes = async (): Promise<Operacao[]> => {
    const { data } = await api.get<Operacao[]>('/v1/operacoes');
    return data;
};

export const getOperacaoById = async (id: string): Promise<Operacao> => {
    const { data } = await api.get<Operacao>(`/v1/operacoes/${id}`);
    return data;
};

export const updateOperacao = async (id: string, operacao: CriarOperacaoDTO): Promise<void> => {
    await api.put(`/v1/operacoes/${id}`, operacao);
};

export const deleteOperacao = async (id: string): Promise<void> => {
    await api.delete(`/v1/operacoes/${id}`);
};

export const createOperacao = async (operacao: CriarOperacaoDTO): Promise<void> => {
    await api.post('/v1/operacoes', operacao);
};