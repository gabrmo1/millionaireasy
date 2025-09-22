import api from './api';
import type { LoginRequest, RegisterRequest } from '../types/auth';

export const login = async (credentials: LoginRequest): Promise<void> => {
    await api.post('/api/v1/auth/login', credentials);
};

export const register = async (userData: RegisterRequest): Promise<void> => {
    await api.post('/api/v1/auth/register', userData);
};

export const logout = async (): Promise<void> => {
    await api.post('/api/v1/auth/logout');
};

export const checkSession = async (): Promise<any> => {
    const { data } = await api.get('/api/v1/auth/me');
    return data;
};