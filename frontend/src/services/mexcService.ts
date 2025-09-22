import api from './api';
import type { SymbolInfo } from '../types/mexc';

export const getStablecoinPairs = async (): Promise<SymbolInfo[]> => {
    const { data } = await api.get<SymbolInfo[]>('/v1/mexc-data/stablecoin-pairs');
    return data.sort((a, b) => a.symbol.localeCompare(b.symbol));
};

export const getStablecoins = async (): Promise<string[]> => {
    const { data } = await api.get<string[]>('/v1/mexc-data/stablecoins');
    return data.sort();
};