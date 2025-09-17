import api from './api';
import type { Estrategia, CriarEstrategiaDTO } from '../types/estrategia';

export const getEstrategias = async (): Promise<Estrategia[]> => {
    const { data } = await api.get<Estrategia[]>('/v1/estrategias');
    return data;
};

export const getEstrategiaById = async (id: string): Promise<Estrategia> => {
    const { data } = await api.get<Estrategia>(`/v1/estrategias/${id}`);
    return data;
};

export const createEstrategia = async (estrategia: CriarEstrategiaDTO): Promise<void> => {
    await api.post('/v1/estrategias', estrategia);
};

export const updateEstrategia = async (id: string, estrategia: CriarEstrategiaDTO): Promise<void> => {
    await api.put(`/v1/estrategias/${id}`, estrategia);
};

export const deleteEstrategia = async (id: string): Promise<void> => {
    await api.delete(`/v1/estrategias/${id}`);
};