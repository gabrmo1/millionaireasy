import api from './api';
import type {Operador, CriarOperadorDTO} from '../types/operador';

export const getOperadores = async (): Promise<Operador[]> => {
    const { data } = await api.get<Operador[]>('/v1/operadores');
    return data;
};

export const getOperadorById = async (id: string): Promise<Operador> => {
    const { data } = await api.get<Operador>(`/v1/operadores/${id}`);
    return data;
};

export const createOperador = async (operador: CriarOperadorDTO): Promise<void> => {
    await api.post('/v1/operadores', operador);
};

export const updateOperador = async (id: string, operador: CriarOperadorDTO): Promise<void> => {
    await api.put(`/v1/operadores/${id}`, operador);
};

export const deleteOperador = async (id: string): Promise<void> => {
    await api.delete(`/v1/operadores/${id}`);
};