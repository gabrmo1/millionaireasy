// src/hooks/useMonitoramento.ts
import { useState, useEffect, useCallback } from 'react';
import { getMonitoramentoData } from '../services/operacaoService';
import type { MonitoramentoDataDTO, CandleChartDTO, EventoChartDTO, IndicadorPointDTO } from '../types/monitoramento';

// ⚡ Factory isolada para gerar massa de dados coerente para o Lightweight Charts
const generateMockMonitoramentoData = (): MonitoramentoDataDTO => {
    const baseTime = Math.floor(Date.now() / 1000) - (100 * 900); // Começa 100 candles de 15m atrás
    const candles: CandleChartDTO[] = [];
    const eventos: EventoChartDTO[] = [];
    const ema20: IndicadorPointDTO[] = [];
    const rsi: IndicadorPointDTO[] = [];

    let lastClose = 62000; // Preço inicial fictício do Bitcoin

    for (let i = 0; i < 100; i++) {
        const time = baseTime + (i * 900);
        const open = lastClose;
        const close = open + (Math.random() * 400 - 200);
        const high = Math.max(open, close) + Math.random() * 100;
        const low = Math.min(open, close) - Math.random() * 100;
        const volume = Math.random() * 15;

        candles.push({ time, open, high, low, close, volume });

        // Simulação suave de Média Móvel (Sobreposta no gráfico principal)
        ema20.push({ time, value: close - (Math.random() * 50 - 25) });

        // Simulação de Oscilador (RSI - Renderizado em painel inferior)
        rsi.push({ time, value: 30 + Math.random() * 40 });

        // Injetando eventos estratégicos (Compras nos fundos, Vendas nos topos)
        if (i === 15) eventos.push({ time, tipo: 'COMPRA', preco: close, tooltip: 'RSI cruzou linha 30', cor: '#26a69a' });
        if (i === 45) eventos.push({ time, tipo: 'VENDA', preco: close, tooltip: 'Take Profit alcançado (10%)', cor: '#ef5350' });
        if (i === 65) eventos.push({ time, tipo: 'COMPRA', preco: close, tooltip: 'Cruzamento de Médias', cor: '#26a69a' });
        if (i === 85) eventos.push({ time, tipo: 'VENDA', preco: close, tooltip: 'Trailing Stop Acionado', cor: '#ef5350' });

        lastClose = close;
    }

    return {
        par: 'BTCUSDT',
        intervalo: '15m',
        nomeEstrategia: 'Estratégia Mockada (RSI + EMA)',
        lucroTotal: 1250.45,
        candles,
        eventos,
        indicadores: {
            'EMA 20': ema20, // Identificado como Overlay pelo 'TradingChart'
            'RSI (14)': rsi  // Identificado como Oscilador pelo 'TradingChart'
        }
    };
};

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
        if (!open || !operacaoId) {
            setData(null);
            return;
        }

        let isMounted = true;
        setLoading(true);
        setError(null);

        // ⚡ SHORT-CIRCUIT: Interceptador de Mock
        if (operacaoId.startsWith('mock-')) {
            // setTimeout leve apenas para simular transição de loading da UI
            setTimeout(() => {
                if (isMounted) {
                    setData(generateMockMonitoramentoData());
                    setLoading(false);
                }
            }, 600);
            return;
        }

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