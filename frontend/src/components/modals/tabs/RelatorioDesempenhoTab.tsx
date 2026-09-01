import React, { memo } from 'react';
import { Box, Typography, CircularProgress, Paper, Divider } from '@mui/material';
import Grid from '@mui/material/GridLegacy';
import { useRelatorioDesempenho } from '../../../hooks/useRelatorioDesempenho';

interface RelatorioDesempenhoTabProps {
    operacaoId: string | null;
    active: boolean;
}

// Formatador seguro: Transforma null/undefined no fallback exigido ("N/A")
const formatValue = (val: string | number | null | undefined, prefix = "", suffix = ""): string => {
    if (val === null || val === undefined || val === '') return "N/A";
    return `${prefix}${val}${suffix}`;
};

const RelatorioDesempenhoTab: React.FC<RelatorioDesempenhoTabProps> = memo(({ operacaoId, active }) => {
    const { data, loading, error } = useRelatorioDesempenho(operacaoId, active);

    if (!active) return null;
    if (loading) return <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}><CircularProgress /></Box>;
    if (error) return <Typography color="error">{error}</Typography>;
    if (!data) return <Typography color="text.secondary">Nenhum dado disponível.</Typography>;

    const RowInfo = ({ label, value, strong = false }: { label: string, value: string, strong?: boolean }) => (
        <Box sx={{ display: 'flex', justifyContent: 'space-between', py: 0.5 }}>
            <Typography variant="body2" color="text.secondary">{label}</Typography>
            <Typography variant="body2" fontWeight={strong ? 'bold' : 'regular'}>{value}</Typography>
        </Box>
    );

    return (
        <Box sx={{ p: 3, overflowY: 'auto', flex: 1 }}>
            <Typography variant="subtitle1" fontWeight="bold" gutterBottom>
                Período: (de {formatValue(data.dataInicio)} até {formatValue(data.dataFim)})
            </Typography>
            <Divider sx={{ mb: 2 }} />

            <Grid container spacing={4}>
                {/* Coluna Esquerda: Lucros e Operações Vencedoras */}
                <Grid item xs={12} md={6}>
                    <Paper variant="outlined" sx={{ p: 2, mb: 2 }}>
                        <RowInfo label="Saldo líquido total" value={formatValue(data.saldoLiquidoTotal, "$")} strong />
                        <RowInfo label="Lucro bruto" value={formatValue(data.lucroBruto, "$")} />
                        <RowInfo label="Fator de lucro" value={formatValue(data.fatorLucro)} />
                        <RowInfo label="Média de lucro/prejuízo" value={formatValue(data.mediaLucroPrejuizo, "$")} />
                    </Paper>

                    <Paper variant="outlined" sx={{ p: 2, mb: 2 }}>
                        <RowInfo label="Maior operação vencedora" value={formatValue(data.maiorOperacaoVencedora, "$")} />
                        <RowInfo label="Média de operações vencedoras" value={formatValue(data.mediaOperacoesVencedoras, "$")} />
                        <RowInfo label="Operações vencedoras" value={formatValue(data.operacoesVencedoras)} />
                        <RowInfo label="Maior sequência vencedora" value={formatValue(data.maiorSequenciaVencedora)} />
                        <RowInfo label="Total de operações" value={formatValue(data.totalOperacoes)} strong />
                        <RowInfo label="Percentual de operações vencedoras" value={formatValue(data.percentualOperacoesVencedoras, "", "%")} />
                        <RowInfo label="Média de tempo em operações vencedoras" value={formatValue(data.mediaTempoOperacoesVencedoras)} />
                        <RowInfo label="Média de tempo em operações" value={formatValue(data.mediaTempoOperacoes)} />
                    </Paper>

                    <Paper variant="outlined" sx={{ p: 2 }}>
                        <Typography variant="subtitle2" fontWeight="bold" sx={{ mb: 1 }}>Declínio máximo (topo ao fundo)</Typography>
                        <RowInfo label="Valor" value={formatValue(data.declinioMaximoValor, "$")} />
                        <RowInfo label="Drawdown máximo (%)" value={formatValue(data.declinioMaximoPercentual, "", "%")} />
                    </Paper>
                </Grid>

                {/* Coluna Direita: Prejuízos e Métricas Gerais */}
                <Grid item xs={12} md={6}>
                    <Paper variant="outlined" sx={{ p: 2, mb: 2 }}>
                        <RowInfo label="Saldo Total" value={formatValue(data.saldoTotal, "$")} strong />
                        <RowInfo label="Prejuízo bruto" value={formatValue(data.prejuizoBruto, "$")} />
                        <RowInfo label="Retorno no capital inicial" value={formatValue(data.retornoCapitalInicial, "", "%")} />
                        <RowInfo label="Custos" value={formatValue(data.custos, "$")} />
                    </Paper>

                    <Paper variant="outlined" sx={{ p: 2, mb: 2 }}>
                        <RowInfo label="Maior operação perdedora" value={formatValue(data.maiorOperacaoPerdedora, "$")} />
                        <RowInfo label="Média de operações perdedoras" value={formatValue(data.mediaOperacoesPerdedoras, "$")} />
                        <RowInfo label="Operações perdedoras" value={formatValue(data.operacoesPerdedoras)} />
                        <RowInfo label="Maior sequência perdedora" value={formatValue(data.maiorSequenciaPerdedora)} />
                        <RowInfo label="Operações zeradas" value={formatValue(data.operacoesZeradas)} />
                        <RowInfo label="Percentual de operações perdedora" value={formatValue(data.percentualOperacoesPerdedoras, "", "%")} />
                        <RowInfo label="Média de tempo em operações perdedoras" value={formatValue(data.mediaTempoOperacoesPerdedoras)} />
                    </Paper>

                    <Paper variant="outlined" sx={{ p: 2 }}>
                        <Typography variant="subtitle2" fontWeight="bold" sx={{ mb: 1 }}>Média da excursão adversa máxima</Typography>
                        <RowInfo label="Valor" value={formatValue(data.maeValor, "$")} />
                        <RowInfo label="MAE (%)" value={formatValue(data.maePercentual, "", "%")} />
                    </Paper>
                </Grid>
            </Grid>
        </Box>
    );
});

export default RelatorioDesempenhoTab;