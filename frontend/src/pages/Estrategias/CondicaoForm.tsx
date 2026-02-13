import React, { useMemo } from 'react';
import { Box, IconButton, Paper, Typography, MenuItem, FormControl, InputLabel, Select, TextField, InputAdornment, FormHelperText } from '@mui/material';
import Grid from '@mui/material/GridLegacy';
import DeleteIcon from '@mui/icons-material/Delete';
import { OperadorLogico, TipoOperando, OperadorComparacao, TipoIndicador } from '../../types/enums';
import type { CondicaoUI, IndicadorConfigUI } from '../../types/estrategia';
import { getTipoOperandoOptions, getOperadorComparacaoOptions, indicatorProperties, type IndicatorUnit } from '../../utils/enumMappings';

interface CondicaoFormProps {
    condicao: CondicaoUI;
    index: number;
    totalCondicoes: number;
    indicadores: IndicadorConfigUI[];
    onUpdate: (index: number, updatedCondicao: CondicaoUI) => void;
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

    const isRsiIndicator = (referencia?: string): boolean => {
        if (!referencia) return false;
        const indicator = indicadores.find(i => i.alias === referencia);
        return !!indicator && (
            indicator.tipoIndicador === TipoIndicador.RSI_CURTO ||
            indicator.tipoIndicador === TipoIndicador.RSI_MEDIO ||
            indicator.tipoIndicador === TipoIndicador.RSI_LONGO ||
            indicator.tipoIndicador === TipoIndicador.RSI_ESTOCASTICO_D ||
            indicator.tipoIndicador === TipoIndicador.RSI_ESTOCASTICO_K
        );
    };

    const handleChange = (field: keyof CondicaoUI, value: any) => {
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

    const handleValorFixoChange = (lado: 'A' | 'B', value: string) => {
        const valorKey = lado === 'A' ? 'operandoAValor' : 'operandoBValor';
        const tipoKey = lado === 'A' ? 'operandoATipo' : 'operandoBTipo';

        const otherSideTipoKey = lado === 'A' ? 'operandoBTipo' : 'operandoATipo';
        const otherSideRefKey = lado === 'A' ? 'operandoBReferencia' : 'operandoAReferencia';

        // Sanitiza o input para previnir valores como "--" ou "5-5"
        const sanitizedValue = value.replace(/(?!^-)[^0-9.]/g, "");

        let finalValue: number | undefined = undefined;

        if (sanitizedValue !== '' && sanitizedValue !== '-') {
            let numValue = parseFloat(sanitizedValue);

            if (!isNaN(numValue)) {
                if (condicao[tipoKey] === TipoOperando.VALOR_FIXO &&
                    condicao[otherSideTipoKey] === TipoOperando.INDICADOR &&
                    isRsiIndicator(condicao[otherSideRefKey])) {

                    if (numValue > 100) numValue = 100;
                    if (numValue < 0) numValue = 0;
                }
                finalValue = numValue;
            }
        }

        handleChange(valorKey, finalValue);
    };

    const renderOperandoInput = (lado: 'A' | 'B') => {
        const tipoKey = lado === 'A' ? 'operandoATipo' : 'operandoBTipo';
        const refKey = lado === 'A' ? 'operandoAReferencia' : 'operandoBReferencia';
        const valorKey = lado === 'A' ? 'operandoAValor' : 'operandoBValor';
        const errorKeyValor = `condicao_${tipoCondicao.toLowerCase()}_${index}_valor_${lado.toLowerCase()}`;
        const errorKeyReferencia = `condicao_${tipoCondicao.toLowerCase()}_${index}_referencia_${lado.toLowerCase()}`;

        const otherSideTipo = lado === 'A' ? condicao.operandoBTipo : condicao.operandoATipo;
        const otherSideRef = lado === 'A' ? condicao.operandoBReferencia : condicao.operandoAReferencia;
        const otherSideUnit = getOperandUnit(otherSideTipo, otherSideRef);
        const preventNegative = otherSideUnit === 'PRICE' || otherSideUnit === 'VOLUME' || otherSideUnit === 'OSCILLATOR_0_100';

        const isComparedToRsi = (otherSideTipo === TipoOperando.INDICADOR && isRsiIndicator(otherSideRef));

        const getFilteredIndicators = () => {
            if (lado === 'A') {
                return indicadores;
            }
            const unitA = getOperandUnit(condicao.operandoATipo, condicao.operandoAReferencia);
            if (!unitA || unitA === 'FIXED') {
                return indicadores;
            }
            return indicadores.filter(ind => {
                const unitInd = indicatorProperties[ind.tipoIndicador]?.unit;
                return unitInd === unitA;
            });
        };

        const inputComponent = () => {
            switch (condicao[tipoKey]) {
                case TipoOperando.INDICADOR:
                    return (
                        <FormControl fullWidth size="small" error={!!errors[errorKeyReferencia]}>
                            <InputLabel>Indicador</InputLabel>
                            <Select
                                value={condicao[refKey] || ''}
                                label="Indicador"
                                onChange={(e) => handleChange(refKey, e.target.value)}
                            >
                                {getFilteredIndicators().map(i => <MenuItem key={i.alias} value={i.alias}>{i.alias}</MenuItem>)}
                            </Select>
                            {errors[errorKeyReferencia] && <FormHelperText>{errors[errorKeyReferencia]}</FormHelperText>}
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
                            onChange={(e) => handleValorFixoChange(lado, e.target.value)}
                            error={!!errors[errorKeyValor]}
                            helperText={errors[errorKeyValor] || ' '}
                            inputProps={{
                                min: preventNegative || isComparedToRsi ? 0 : undefined,
                                max: isComparedToRsi ? 100 : undefined,
                            }}
                            InputProps={{
                                endAdornment: isComparedToRsi && (
                                    <InputAdornment position="end">
                                        {tipoCondicao === 'Compra' ? 'sobrevendido' : 'sobrecomprado'}
                                    </InputAdornment>
                                )
                            }}
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

    const filteredOptionsB = useMemo(() => {
        const unitA = getOperandUnit(condicao.operandoATipo, condicao.operandoAReferencia);

        return getTipoOperandoOptions().filter(opt => {
            if (condicao.operandoATipo === TipoOperando.VALOR_FIXO && opt.value === TipoOperando.VALOR_FIXO) {
                return false;
            }
            if (unitA && unitA !== 'FIXED') {
                if (opt.value === TipoOperando.VALOR_FIXO) return true;

                if (opt.value === TipoOperando.INDICADOR) {
                    return indicadores.some(ind => indicatorProperties[ind.tipoIndicador]?.unit === unitA);
                }

                const optUnit = getOperandUnit(opt.value as TipoOperando, undefined);
                return unitA === optUnit;
            }
            return true;
        });
    }, [condicao.operandoATipo, condicao.operandoAReferencia, indicadores]);


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
                                {filteredOptionsB.map(opt => <MenuItem key={opt.value} value={opt.value}>{opt.label}</MenuItem>)}
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