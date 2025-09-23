import type { IndicadorConfigDTO } from '../../types/estrategia';
import { tipoIndicadorLabels } from '../../utils/enumMappings';
import { indicadorParamsConfig } from './indicadorParamsConfig';

export const generateIndicatorAlias = (indicator: IndicadorConfigDTO): string => {
    const baseName = tipoIndicadorLabels[indicator.tipoIndicador] || 'Indicador';
    const params = indicator.parametros;
    if (!params || Object.keys(params).length === 0) {
        return baseName;
    }

    const config = indicadorParamsConfig[indicator.tipoIndicador];
    if (!config) return baseName;

    const details = config.map(p => {
        const value = params[p.key];
        let suffix = 'p';
        if (p.key.toLowerCase().includes('suavizacaok')) suffix = 'k';
        if (p.key.toLowerCase().includes('suavizacaod')) suffix = 'd';
        return `${value || 0}${suffix}`;
    }).join(' / ');

    return `${baseName} - ${details}`;
};