import { useState, useEffect } from 'react';
import axios from 'axios';
import { getRelatorioDesempenho } from '../services/operacaoService';
import type { RelatorioDesempenhoDTO } from '../types/monitoramento';

interface RequestState<T> {
    data: T | null;
    loading: boolean;
    error: string | null;
}

const generateMockRelatorio = (): RelatorioDesempenhoDTO => ({
    dataInicio: "2026-01-01T00:00:00.000Z",
    dataFim: "2026-07-28T23:59:59.000Z",
    saldoLiquidoTotal: 1250.45,
    lucroBruto: 2000.00,
    fatorLucro: 1.66,
    mediaLucroPrejuizo: 25.50,
    maiorOperacaoVencedora: 150.00,
    mediaOperacoesVencedoras: 45.00,
    operacoesVencedoras: 44,
    maiorSequenciaVencedora: 5,
    totalOperacoes: 68,
    percentualOperacoesVencedoras: 64.7,
    mediaTempoOperacoesVencedoras: "2h 15m",
    mediaTempoOperacoes: "3h 45m",
    declinioMaximoValor: -350.00,
    declinioMaximoPercentual: 15.5,
    saldoTotal: 6250.45,
    prejuizoBruto: 749.55,
    retornoCapitalInicial: 25.01,
    custos: 45.30,
    maiorOperacaoPerdedora: -80.00,
    mediaOperacoesPerdedoras: -31.23,
    operacoesPerdedoras: 24,
    maiorSequenciaPerdedora: 3,
    operacoesZeradas: 0,
    percentualOperacoesPerdedoras: 35.3,
    mediaTempoOperacoesPerdedoras: "4h 10m",
    maeValor: -120.00,
    maePercentual: 5.2
});

export const useRelatorioDesempenho = (operacaoId: string | null, active: boolean) => {
    const [state, setState] = useState<RequestState<RelatorioDesempenhoDTO>>({
        data: null,
        loading: false,
        error: null,
    });

    useEffect(() => {
        // Bail out imediato se a aba não estiver ativa
        if (!active || !operacaoId) return;

        // ⚡ SHORT-CIRCUIT: Intercepta IDs de simulação e retorna o mock sincronicamente
        if (operacaoId.startsWith('mock-')) {
            setState({ data: generateMockRelatorio(), loading: false, error: null });
            return;
        }

        const abortController = new AbortController();

        const fetchRelatorio = async () => {
            setState(prev => ({ ...prev, loading: true, error: null }));
            try {
                const result = await getRelatorioDesempenho(operacaoId, abortController.signal);
                if (!abortController.signal.aborted) {
                    setState({ data: result, loading: false, error: null });
                }
            } catch (err: any) {
                // Intercepta Axios Cancel e DOM AbortError nativo
                if (axios.isCancel(err) || err.name === 'AbortError' || err.name === 'CanceledError') {
                    console.warn(`[Performance] Fetch abortado para economizar recursos (Operação: ${operacaoId})`);
                    return;
                }

                // Evita memory leak ou state update em componentes desmontados
                if (!abortController.signal.aborted) {
                    console.error("[RelatorioDesempenho Hook Error]:", err);
                    const message = err.response?.data?.message || "Falha ao buscar o relatório de desempenho.";
                    setState(prev => ({ ...prev, loading: false, error: message }));
                }
            }
        };

        fetchRelatorio();

        return () => {
            // Limpeza essencial ao trocar de aba (unmount visual)
            abortController.abort();
        };
    }, [operacaoId, active]);

    return state;
};