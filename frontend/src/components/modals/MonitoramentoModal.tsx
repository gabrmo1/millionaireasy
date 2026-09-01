import React, { useState, useMemo } from 'react';
import {
    Dialog,
    DialogTitle,
    DialogContent,
    IconButton,
    Box,
    Typography,
    useTheme,
    Paper,
    Button,
    Snackbar,
    Alert,
    CircularProgress,
    Tabs,
    Tab
} from '@mui/material';
import Grid from '@mui/material/GridLegacy';
import {
    Close as CloseIcon,
    Refresh as RefreshIcon,
    TrendingUp,
    ShoppingCart,
    Sell
} from '@mui/icons-material';
import TradingChart from '../chart/TradingChart';
import { useMonitoramento } from '../../hooks/useMonitoramento';
import RelatorioDesempenhoTab from './tabs/RelatorioDesempenhoTab';

interface MonitoramentoModalProps {
    open: boolean;
    onClose: () => void;
    operacaoId: string | null;
}

const MonitoramentoModal: React.FC<MonitoramentoModalProps> = ({ open, onClose, operacaoId }) => {
    const theme = useTheme();
    const { data, loading, error, refresh, clearError } = useMonitoramento(operacaoId, open);

    // Estado isolado para o controle de abas
    const [activeTab, setActiveTab] = useState(0);

    // Memoização das estatísticas rápidas do rodapé para evitar recálculo a cada tick
    const stats = useMemo(() => {
        if (!data?.eventos) return { compras: 0, vendas: 0 };
        return data.eventos.reduce((acc, evento) => {
            if (evento.tipo === 'COMPRA') acc.compras++;
            if (evento.tipo === 'VENDA') acc.vendas++;
            return acc;
        }, { compras: 0, vendas: 0 });
    }, [data?.eventos]);

    return (
        <Dialog
            open={open}
            onClose={onClose}
            maxWidth="xl"
            fullWidth
            PaperProps={{ sx: { height: '90vh', display: 'flex', flexDirection: 'column' } }}
        >
            <DialogTitle sx={{ p: 0 }}>
                {/* Header Superior com Título e Ações */}
                <Box sx={{ p: 2, display: 'flex', justifyContent: 'center', position: 'relative', borderBottom: 1, borderColor: 'divider' }}>
                    <Typography variant="h6" fontWeight="bold">
                        {loading ? "Carregando Operação..." : (
                            `Monitoramento: ${data?.par || ''} (${data?.intervalo || ''}) - ${data?.nomeEstrategia || ''}`
                        )}
                    </Typography>
                    <Box sx={{ position: 'absolute', right: 16, top: '50%', transform: 'translateY(-50%)', display: 'flex', gap: 1 }}>
                        <Button
                            startIcon={<RefreshIcon />}
                            variant="contained"
                            size="small"
                            onClick={refresh}
                            disabled={loading || !operacaoId}
                        >
                            Atualizar
                        </Button>
                        <IconButton onClick={onClose} size="small">
                            <CloseIcon />
                        </IconButton>
                    </Box>
                </Box>

                {/* Navegação de Abas controlada via estado local */}
                <Box sx={{ borderBottom: 1, borderColor: 'divider', bgcolor: theme.palette.background.paper }}>
                    <Tabs
                        value={activeTab}
                        onChange={(_, newVal) => setActiveTab(newVal)}
                        centered
                        indicatorColor="primary"
                        textColor="primary"
                    >
                        <Tab label="Gráfico e Histórico" />
                        <Tab label="Relatório de Desempenho" />
                    </Tabs>
                </Box>
            </DialogTitle>

            <DialogContent sx={{
                p: 0,
                display: 'flex',
                flexDirection: 'column',
                flex: 1,
                overflow: 'hidden',
                backgroundColor: theme.palette.background.default
            }}>
                {loading ? (
                    <Box sx={{ display: 'flex', flex: 1, justifyContent: 'center', alignItems: 'center', flexDirection: 'column', gap: 2 }}>
                        <CircularProgress />
                        <Typography color="text.secondary">Calculando métricas históricas...</Typography>
                    </Box>
                ) : !data ? (
                    <Box sx={{ display: 'flex', flex: 1, justifyContent: 'center', alignItems: 'center' }}>
                        <Typography color="text.secondary">Selecione uma operação para visualizar os detalhes.</Typography>
                    </Box>
                ) : (
                    <>
                        {/* Aba 0: Gráfico e Histórico (Preservado no DOM via CSS) */}
                        <Box sx={{ display: activeTab === 0 ? 'flex' : 'none', flex: 1, flexDirection: 'column', minHeight: 0 }}>
                            {/* Área do Gráfico */}
                            <Box sx={{ display: 'flex', flex: 1, minHeight: 0 }}>
                                <TradingChart data={data} />
                            </Box>

                            {/* Rodapé de Estatísticas Rápidas */}
                            <Box sx={{ p: 2, borderTop: `1px solid ${theme.palette.divider}`, bgcolor: theme.palette.background.paper }}>
                                <Grid container spacing={2}>
                                    <Grid item xs={12} sm={4}>
                                        <Paper variant="outlined" sx={{ p: 2, display: 'flex', alignItems: 'center', gap: 2, borderColor: data.lucroTotal >= 0 ? 'success.main' : 'error.main' }}>
                                            <TrendingUp color={data.lucroTotal >= 0 ? "success" : "error"} />
                                            <Box>
                                                <Typography variant="caption" color="text.secondary">LUCRO ACUMULADO</Typography>
                                                <Typography variant="h6" fontWeight="bold">
                                                    {data.lucroTotal >= 0 ? '+' : ''}${data.lucroTotal.toFixed(2)}
                                                </Typography>
                                            </Box>
                                        </Paper>
                                    </Grid>
                                    <Grid item xs={12} sm={4}>
                                        <Paper variant="outlined" sx={{ p: 2, display: 'flex', alignItems: 'center', gap: 2 }}>
                                            <ShoppingCart color="primary" />
                                            <Box>
                                                <Typography variant="caption" color="text.secondary">ORDENS DE COMPRA</Typography>
                                                <Typography variant="h6" fontWeight="bold">{stats.compras}</Typography>
                                            </Box>
                                        </Paper>
                                    </Grid>
                                    <Grid item xs={12} sm={4}>
                                        <Paper variant="outlined" sx={{ p: 2, display: 'flex', alignItems: 'center', gap: 2 }}>
                                            <Sell color="secondary" />
                                            <Box>
                                                <Typography variant="caption" color="text.secondary">ORDENS DE VENDA</Typography>
                                                <Typography variant="h6" fontWeight="bold">{stats.vendas}</Typography>
                                            </Box>
                                        </Paper>
                                    </Grid>
                                </Grid>
                            </Box>
                        </Box>

                        {/* Aba 1: Relatório de Desempenho (Renderização e Fetch Lazily) */}
                        <RelatorioDesempenhoTab
                            operacaoId={operacaoId}
                            active={activeTab === 1}
                        />
                    </>
                )}
            </DialogContent>

            <Snackbar open={!!error} autoHideDuration={5000} onClose={clearError}>
                <Alert onClose={clearError} severity="error" variant="filled">
                    {error}
                </Alert>
            </Snackbar>
        </Dialog>
    );
};

export default MonitoramentoModal;