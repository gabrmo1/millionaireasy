import api from './api';
import type {
    Estrategia,
    EstrategiaFormData,
    CriarEstrategiaRequestDTO,
    IndicadorConfigUI,
    CondicaoUI,
    IndicadorConfigDTO,
    CondicaoDTO
} from '../types/estrategia';

export const getEstrategias = async (): Promise<Estrategia[]> => {
    const { data } = await api.get<Estrategia[]>('/v1/estrategias');
    return data;
};

export const getEstrategiaById = async (id: string): Promise<Estrategia> => {
    const { data } = await api.get<Estrategia>(`/v1/estrategias/${id}`);
    return data;
};

export const createEstrategia = async (estrategia: EstrategiaFormData): Promise<void> => {
    const payload = mapToRequestDTO(estrategia);
    await api.post('/v1/estrategias', payload);
};

export const updateEstrategia = async (id: string, estrategia: EstrategiaFormData): Promise<void> => {
    const payload = mapToRequestDTO(estrategia);
    await api.put(`/v1/estrategias/${id}`, payload);
};

export const deleteEstrategia = async (id: string): Promise<void> => {
    await api.delete(`/v1/estrategias/${id}`);
};

const mapToRequestDTO = (form: EstrategiaFormData): CriarEstrategiaRequestDTO => {
    return {
        nome: form.nome,
        valorOperacaoFixo: form.valorOperacaoFixo,
        stablecoin: form.stablecoin || 'USDT', // Default seguro
        percentualValorOperacao: form.percentualValorOperacao,
        vendaApenasPorLucro: form.vendaApenasPorLucro,
        percentualLucro: form.percentualLucro,
        indicadoresConfig: form.indicadoresConfig.map(cleanIndicador),
        condicoesCompra: form.condicoesCompra.map(cleanCondicao),
        condicoesVenda: form.condicoesVenda.map(cleanCondicao)
    };
};

const cleanIndicador = (item: IndicadorConfigUI): IndicadorConfigDTO => {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { clientId, ...dto } = item;
    return dto;
};

const cleanCondicao = (item: CondicaoUI): CondicaoDTO => {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { clientId, ...dto } = item;
    return dto;
};