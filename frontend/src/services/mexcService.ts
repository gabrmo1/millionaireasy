import api from './api';
import type { SymbolInfo, PaginatedResponse } from '../types/mexc';

export const getStablecoinPairs = async (quoteAsset: string, page = 0, size = 50, searchTerm = ''): Promise<PaginatedResponse<SymbolInfo>> => {
    const { data } = await api.get<PaginatedResponse<SymbolInfo>>('/v1/mexc-data/stablecoin-pairs', {
        params: { page, size, sort: 'symbol', quoteAsset, searchTerm }
    });
    return data;
};

export const getStablecoins = async (): Promise<string[]> => {
    const { data } = await api.get<string[]>('/v1/mexc-data/stablecoins');
    return data.sort();
};