import React, { createContext, useState, useContext, useMemo, type ReactNode } from 'react';
import { login as loginService, logout as logoutService, register as registerService } from '../services/authService';
import type { LoginRequest, RegisterRequest } from '../types/auth';

interface AuthContextType {
    isAuthenticated: boolean;
    login: (credentials: LoginRequest) => Promise<void>;
    register: (credentials: RegisterRequest) => Promise<void>;
    logout: () => Promise<void>;
    checkAuth: () => void; // Função para verificar o estado inicial
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
    // O estado agora é um simples booleano. O cookie é a fonte da verdade.
    const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);

    const login = async (credentials: LoginRequest) => {
        await loginService(credentials);
        setIsAuthenticated(true);
    };

    const register = async (credentials: RegisterRequest) => {
        await registerService(credentials);
        setIsAuthenticated(true);
    };

    const logout = async () => {
        try {
            await logoutService();
        } catch (error) {
            console.error("Logout falhou, limpando o estado local de qualquer maneira.", error);
        } finally {
            setIsAuthenticated(false);
        }
    };

    // Esta função pode ser chamada no início da aplicação para verificar se o cookie ainda é válido.
    // Para simplificar, estamos assumindo que o usuário está deslogado ao iniciar.
    // Uma implementação mais robusta faria uma chamada a um endpoint `/api/v1/users/me` para validar o cookie.
    const checkAuth = () => {
        // Lógica para verificar o cookie no backend seria implementada aqui.
        // Por enquanto, o estado inicial é `false`.
    };

    const value = useMemo(() => ({
        isAuthenticated,
        login,
        register,
        logout,
        checkAuth
    }), [isAuthenticated]);

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = (): AuthContextType => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};