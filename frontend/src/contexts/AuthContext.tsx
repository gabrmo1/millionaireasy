import React, { createContext, useState, useContext, useMemo, type ReactNode, useEffect } from 'react';
import { login as loginService, logout as logoutService, register as registerService, checkSession } from '../services/authService';
import type { LoginRequest, RegisterRequest } from '../types/auth';

interface AuthContextType {
    isAuthenticated: boolean;
    isLoading: boolean;
    login: (credentials: LoginRequest) => Promise<void>;
    register: (credentials: RegisterRequest) => Promise<void>;
    logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
    const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
    const [isLoading, setIsLoading] = useState<boolean>(true); // Começa como true

    useEffect(() => {
        const verifyAuth = async () => {
            try {
                await checkSession();
                setIsAuthenticated(true);
            } catch (error) {
                setIsAuthenticated(false);
            } finally {
                setIsLoading(false);
            }
        };

        verifyAuth();
    }, []);

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

    const value = useMemo(() => ({
        isAuthenticated,
        isLoading,
        login,
        register,
        logout,
    }), [isAuthenticated, isLoading]);

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