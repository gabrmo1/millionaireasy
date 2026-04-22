// src/hooks/useMonitoramento.ts

import { useState, useEffect, useCallback } from 'react';
import { getMonitoramentoData } from '../services/operacaoService';
import type { MonitoramentoDataDTO } from '../types/monitoramento';

export const useMonitoramento = (operacaoId: string | null, open: boolean) => {
    const [data, setData] = useState<MonitoramentoDataDTO | null>(null);
    const [loading, setLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);
    const [refreshKey, setRefreshKey] = useState<number>(0);

    const refresh = useCallback(() => {
        setRefreshKey(prev => prev + 1);
    }, []);

    const clearError = useCallback(() => setError(null), []);

    useEffect(() => {
        // Se o modal fechar ou não houver ID, limpamos o estado para evitar vazamento de memória ou UI inconsistente
        if (!open || !operacaoId) {
            setData(null);
            return;
        }

        let isMounted = true;
        setLoading(true);
        setError(null);

        const fetchData = async () => {
            try {
                const result = await getMonitoramentoData(operacaoId);
                if (isMounted) {
                    setData(result);
                }
            } catch (err) {
                if (isMounted) {
                    setError("Falha ao recuperar dados da operação. Tente novamente.");
                }
                console.error("[Monitoramento Hook Error]:", err);
            } finally {
                if (isMounted) {
                    setLoading(false);
                }
            }
        };

        fetchData();

        return () => {
            isMounted = false;
        };
    }, [open, operacaoId, refreshKey]);

    return { data, loading, error, refresh, clearError };
};