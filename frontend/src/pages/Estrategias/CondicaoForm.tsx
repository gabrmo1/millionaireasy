import React, { useMemo } from 'react';
import { Box, IconButton, Paper, Typography, MenuItem, FormControl, InputLabel, Select, TextField } from '@mui/material';
import Grid from '@mui/material/GridLegacy';
import DeleteIcon from '@mui/icons-material/Delete';
import { OperadorLogico, TipoOperando, OperadorComparacao } from '../../types/enums';
import type { CondicaoDTO, IndicadorConfigDTO } from '../../types/estrategia';
import { getTipoOperandoOptions, getOperadorComparacaoOptions, indicatorProperties, type IndicatorUnit } from '../../utils/enumMappings';

interface CondicaoFormProps {
    condicao: CondicaoDTO;
    index: number;
    totalCondicoes: number;
    indicadores: IndicadorConfigDTO[];
    onUpdate: (index: number, updatedCondicao: CondicaoDTO) => void;
    onRemove: (index: number) => void;
    tipoCondicao: 'Compra' | 'Venda';
    errors: Record<string, string | null>;
}

const CondicaoForm: React.FC<CondicaoFormProps> = ({ condicao, index, totalCondicoes, indicadores, onUpdate, onRemove, tipoCondicao, errors }) => {

    const getOperandUnit = (tipo: TipoOperando, referencia?: string): IndicatorUnit | 'FIXED' | 'PRICE' | null => {
        if (tipo === TipoOperando.VALOR_FIXO) return 'FIXED';
        if (tipo === TipoOperando.PRECO_FECHAMENTO) return 'PRICE';
        if (tipo === TipoOperando.INDICADOR && referencia) {
            const indicator = indicadores.find(i => i.alias === referencia);
            return indicator ? indicatorProperties[indicator.tipoIndicador]?.unit : null;
        }
        return null;
    };

    const handleChange = (field: keyof CondicaoDTO, value: any) => {
        const updatedCondicao = { ...condicao, [field]: value };

        const unitA = getOperandUnit(updatedCondicao.operandoATipo, updatedCondicao.operandoAReferencia);
        const unitB = getOperandUnit(updatedCondicao.operandoBTipo, updatedCondicao.operandoBReferencia);

        const isCrossover = updatedCondicao.operador === OperadorComparacao.CRUZOU_PARA_CIMA || updatedCondicao.operador === OperadorComparacao.CRUZOU_PARA_BAIXO;

        if ( (unitA && unitB && unitA !== 'FIXED' && unitB !== 'FIXED' && unitA !== unitB) ||
            (isCrossover && unitA === 'FIXED' && unitB === 'FIXED') )
        {
            updatedCondicao.operandoBTipo = TipoOperando.VALOR_FIXO;
            delete updatedCondicao.operandoBReferencia;
            delete updatedCondicao.operandoBValor;
        }

        onUpdate(index, updatedCondicao);
    };

    const renderOperandoInput = (lado: 'A' | 'B') => {
        const tipoKey = lado === 'A' ? 'operandoATipo' : 'operandoBTipo';
        const refKey = lado === 'A' ? 'operandoAReferencia' : 'operandoBReferencia';
        const valorKey = lado === 'A' ? 'operandoAValor' : 'operandoBValor';
        const errorKey = `condicao_${tipoCondicao.toLowerCase()}_${index}_valor_${lado.toLowerCase()}`;

        const otherSideType = lado === 'A' ? condicao.operandoBTipo : condicao.operandoATipo;
        const otherSideRef = lado === 'A' ? condicao.operandoBReferencia : condicao.operandoAReferencia;
        let preventNegative = false;
        const otherSideUnit = getOperandUnit(otherSideType, otherSideRef);
        if (otherSideUnit === 'PRICE' || otherSideUnit === 'VOLUME' || otherSideUnit === 'OSCILLATOR_0_100') {
            preventNegative = true;
        }


        const unitA = getOperandUnit(condicao.operandoATipo, condicao.operandoAReferencia);
        const filteredIndicators = indicadores.filter(ind => {
            if (!unitA || unitA === 'FIXED') return true;
            const unitInd = indicatorProperties[ind.tipoIndicador]?.unit;
            return unitInd === unitA;
        });

        const inputComponent = () => {
            switch (condicao[tipoKey]) {
                case TipoOperando.INDICADOR:
                    return (
                        <FormControl fullWidth size="small">
                            <InputLabel>Indicador</InputLabel>
                            <Select
                                value={condicao[refKey] || ''}
                                label="Indicador"
                                onChange={(e) => handleChange(refKey, e.target.value)}
                            >
                                {filteredIndicators.map(i => <MenuItem key={i.alias} value={i.alias}>{i.alias}</MenuItem>)}
                            </Select>
                        </FormControl>
                    );
                case TipoOperando.VALOR_FIXO:
                    return (
                        <TextField
                            label="Valor"
                            type="number"
                            size="small"
                            fullWidth
                            value={condicao[valorKey] ?? ''}
                            onChange={(e) => handleChange(valorKey, e.target.value === '' ? undefined : parseFloat(e.target.value))}
                            error={!!errors[errorKey]}
                            helperText={errors[errorKey] || ' '}
                            inputProps={preventNegative ? { min: 0 } : undefined}
                        />
                    );
                case TipoOperando.PRECO_FECHAMENTO:
                default:
                    return null;
            }
        };

        return condicao[tipoKey] !== TipoOperando.PRECO_FECHAMENTO ? <Box mt={1.5}>{inputComponent()}</Box> : null;
    };

    const isLastCondition = index === totalCondicoes - 1;

    const filteredOptions = useMemo(() => {
        const unitA = getOperandUnit(condicao.operandoATipo, condicao.operandoAReferencia);
        const isCrossover = condicao.operador === OperadorComparacao.CRUZOU_PARA_CIMA || condicao.operador === OperadorComparacao.CRUZOU_PARA_BAIXO;

        return getTipoOperandoOptions().filter(opt => {
            if (isCrossover && condicao.operandoATipo === TipoOperando.VALOR_FIXO && opt.value === TipoOperando.VALOR_FIXO) {
                return false;
            }
            if (!unitA || unitA === 'FIXED') return true;
            if (opt.value === TipoOperando.VALOR_FIXO) return true;
            if (opt.value === TipoOperando.PRECO_FECHAMENTO) return unitA === 'PRICE';
            return opt.value === TipoOperando.INDICADOR;
        });
    }, [condicao.operandoATipo, condicao.operandoAReferencia, condicao.operador]);

    return (
        <>
            <Paper elevation={3} sx={{ p: 2 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                    <Typography variant="h6">Condição de {tipoCondicao} #{index + 1}</Typography>
                    <IconButton onClick={() => onRemove(index)} color="error"><DeleteIcon /></IconButton>
                </Box>
                <Grid container spacing={2} alignItems="flex-start">
                    <Grid item xs={12} sm={5}>
                        <FormControl fullWidth size="small">
                            <InputLabel>Quando</InputLabel>
                            <Select
                                value={condicao.operandoATipo || ''}
                                label="Quando"
                                onChange={(e) => handleChange('operandoATipo', e.target.value)}
                            >
                                {getTipoOperandoOptions().map(opt => <MenuItem key={opt.value} value={opt.value}>{opt.label}</MenuItem>)}
                            </Select>
                        </FormControl>
                        {renderOperandoInput('A')}
                    </Grid>
                    <Grid item xs={12} sm={2} sx={{ pt: { xs: 2, sm: '16px !important' } }}>
                        <FormControl fullWidth size="small">
                            <InputLabel>Condição</InputLabel>
                            <Select
                                value={condicao.operador || ''}
                                label="Condição"
                                onChange={(e) => handleChange('operador', e.target.value)}
                            >
                                {getOperadorComparacaoOptions().map(opt => <MenuItem key={opt.value} value={opt.value}>{opt.label}</MenuItem>)}
                            </Select>
                        </FormControl>
                    </Grid>
                    <Grid item xs={12} sm={5}>
                        <FormControl fullWidth size="small">
                            <InputLabel>Comparado</InputLabel>
                            <Select
                                value={condicao.operandoBTipo || ''}
                                label="Comparado"
                                onChange={(e) => handleChange('operandoBTipo', e.target.value)}
                            >
                                {filteredOptions.map(opt => <MenuItem key={opt.value} value={opt.value}>{opt.label}</MenuItem>)}
                            </Select>
                        </FormControl>
                        {renderOperandoInput('B')}
                    </Grid>
                </Grid>
            </Paper>

            {!isLastCondition && (
                <Box sx={{ display: 'flex', justifyContent: 'center', my: 1, zIndex: 2, position: 'relative' }}>
                    <FormControl size="small" sx={{ minWidth: 80 }}>
                        <Select
                            value={condicao.operadorParaProxima || 'AND'}
                            onChange={(e) => handleChange('operadorParaProxima', e.target.value as OperadorLogico)}
                            sx={{ borderRadius: '50px', '& .MuiSelect-select': { py: 0.5, px: 2, fontWeight: 'bold', backgroundColor: (theme) => theme.palette.background.paper, }, }}
                        >
                            <MenuItem value={'AND'}>E</MenuItem>
                            <MenuItem value={'OR'}>OU</MenuItem>
                        </Select>
                    </FormControl>
                </Box>
            )}
        </>
    );
};

export default CondicaoForm;