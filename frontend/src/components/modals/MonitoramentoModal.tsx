import React, { useState, useEffect } from 'react';
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
    CircularProgress
} from '@mui/material';
import Grid from '@mui/material/GridLegacy';
import {
    Close as CloseIcon,
    Refresh as RefreshIcon,
    TrendingUp,
    ShoppingCart,
    Sell
} from '@mui/icons-material';
import { getMonitoramentoData } from '../../services/operacaoService';
import type { MonitoramentoDataDTO } from '../../types/monitoramento';
import TradingChart from '../chart/TradingChart';

interface MonitoramentoModalProps {
    open: boolean;
    onClose: () => void;
    operacaoId: string | null;
}

const MonitoramentoModal: React.FC<MonitoramentoModalProps> = ({ open, onClose, operacaoId }) => {
    const theme = useTheme();

    const [data, setData] = useState<MonitoramentoDataDTO | null>(null);
    const [loading, setLoading] = useState(false);
    const [refreshKey, setRefreshKey] = useState(0);
    const [alertMessage, setAlertMessage] = useState("");

    useEffect(() => {
        if (!open || !operacaoId) return;

        let isMounted = true;

        const fetchData = async () => {
            setLoading(true);
            try {
                const result = await getMonitoramentoData(operacaoId);
                if (isMounted) {
                    setData(result);
                }
            } catch (error) {
                if (isMounted) {
                    setAlertMessage("Erro ao buscar dados de monitoramento da operação.");
                    console.error("Erro no monitoramento:", error);
                }
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

    const handleRefresh = () => {
        setRefreshKey(prev => prev + 1);
    };

    return (
        <Dialog
            open={open}
            onClose={onClose}
            maxWidth="xl"
            fullWidth
            PaperProps={{ sx: { height: '90vh', display: 'flex', flexDirection: 'column' } }}
        >
            <DialogTitle sx={{ textAlign: 'center', fontWeight: 'bold', position: 'relative', borderBottom: 1, borderColor: 'divider' }}>
                Monitoramento {data ? `${data.par} ${data.intervalo} - ${data.nomeEstrategia}` : "Carregando..."}

                <Box sx={{ position: 'absolute', right: 16, top: '50%', transform: 'translateY(-50%)', display: 'flex', gap: 1 }}>
                    <Button
                        startIcon={<RefreshIcon />}
                        variant="outlined"
                        size="small"
                        onClick={handleRefresh}
                        disabled={loading}
                    >
                        Atualizar
                    </Button>
                    <IconButton onClick={onClose} size="small"><CloseIcon /></IconButton>
                </Box>
            </DialogTitle>

            <DialogContent sx={{ p: 0, display: 'flex', flexDirection: 'column', flex: 1, overflow: 'hidden', backgroundColor: theme.palette.background.default }}>
                {loading ? (
                    <Box sx={{ display: 'flex', flex: 1, justifyContent: 'center', alignItems: 'center', flexDirection: 'column', gap: 2 }}>
                        <CircularProgress />
                        <Typography color="text.secondary">Sincronizando dados com a corretora...</Typography>
                    </Box>
                ) : !data ? (
                    <Box sx={{ display: 'flex', flex: 1, justifyContent: 'center', alignItems: 'center' }}>
                        <Typography color="text.secondary">Não foi possível carregar os dados de monitoramento.</Typography>
                    </Box>
                ) : (
                    <>
                        <Box sx={{ display: 'flex', flex: 1, minHeight: 0 }}>
                            <TradingChart data={data} />
                        </Box>

                        <Box sx={{ p: 2, borderTop: `1px solid ${theme.palette.divider}`, bgcolor: theme.palette.background.paper }}>
                            <Grid container spacing={3} justifyContent="center">
                                <Grid item xs={12} sm={4}>
                                    <Paper variant="outlined" sx={{ p: 2, display: 'flex', alignItems: 'center', gap: 2, bgcolor: 'rgba(0, 230, 118, 0.05)' }}>
                                        <TrendingUp color="success" fontSize="large" />
                                        <Box>
                                            <Typography variant="caption" color="text.secondary" textTransform="uppercase">Lucro Total</Typography>
                                            <Typography variant="h5" fontWeight="bold" color="success.main">
                                                {data.lucroTotal >= 0 ? '+' : ''} ${data.lucroTotal?.toFixed(2) || '0.00'}
                                            </Typography>
                                        </Box>
                                    </Paper>
                                </Grid>
                                <Grid item xs={12} sm={4}>
                                    <Paper variant="outlined" sx={{ p: 2, display: 'flex', alignItems: 'center', gap: 2 }}>
                                        <ShoppingCart color="error" fontSize="large" />
                                        <Box>
                                            <Typography variant="caption" color="text.secondary" textTransform="uppercase">Compras Realizadas</Typography>
                                            <Typography variant="h5" fontWeight="bold">
                                                {data.eventos.filter(e => e.tipo === 'COMPRA').length}
                                            </Typography>
                                        </Box>
                                    </Paper>
                                </Grid>
                                <Grid item xs={12} sm={4}>
                                    <Paper variant="outlined" sx={{ p: 2, display: 'flex', alignItems: 'center', gap: 2 }}>
                                        <Sell color="success" fontSize="large" />
                                        <Box>
                                            <Typography variant="caption" color="text.secondary" textTransform="uppercase">Vendas (Take Profit)</Typography>
                                            <Typography variant="h5" fontWeight="bold">
                                                {data.eventos.filter(e => e.tipo === 'VENDA').length}
                                            </Typography>
                                        </Box>
                                    </Paper>
                                </Grid>
                            </Grid>
                        </Box>
                    </>
                )}
            </DialogContent>

            <Snackbar
                open={!!alertMessage}
                autoHideDuration={4000}
                onClose={() => setAlertMessage("")}
                anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
                sx={{ position: 'absolute', top: 100 }}
            >
                <Alert onClose={() => setAlertMessage("")} severity="error" sx={{ width: '100%', boxShadow: 3 }}>
                    {alertMessage}
                </Alert>
            </Snackbar>
        </Dialog>
    );
};

export default MonitoramentoModal;