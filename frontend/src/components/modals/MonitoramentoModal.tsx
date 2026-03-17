import React, { useState, useEffect, useRef, useMemo } from 'react';
import {
    Dialog,
    DialogTitle,
    DialogContent,
    IconButton,
    Box,
    Typography,
    useTheme,
    Paper,
    List,
    ListItem,
    ListItemText,
    ListItemButton,
    Divider,
    Button,
    Tooltip,
    Snackbar,
    Alert,
    ToggleButton,
    ToggleButtonGroup
} from '@mui/material';
import Grid from '@mui/material/GridLegacy';
import {
    Close as CloseIcon,
    Refresh as RefreshIcon,
    FormatListBulleted,
    Layers as LayersIcon,
    KeyboardArrowDown,
    KeyboardArrowUp,
    TrendingUp,
    ShoppingCart,
    Sell,
    Visibility,
    VisibilityOff
} from '@mui/icons-material';
import {
    createChart,
    ColorType,
    CrosshairMode,
    type IChartApi,
    type CandlestickData,
    type SeriesMarker,
    type Time,
    CandlestickSeries,
    LineSeries,
    createSeriesMarkers
} from 'lightweight-charts';

interface MonitoramentoModalProps {
    open: boolean;
    onClose: () => void;
    operacaoId: string | null;
}

// ============================================================================
// GERADOR DE DADOS MOCKADOS (100 Candles + EMA + RSI)
// ============================================================================
const gerarMockData = () => {
    const candles = [];
    const rsi = [];
    const ema = [];

    let currentClose = 51000;
    let currentTime = new Date('2024-02-20T00:00:00Z').getTime();

    for (let i = 0; i < 100; i++) {
        const open = currentClose + (Math.random() - 0.5) * 50;
        const high = open + Math.random() * 150;
        const low = open - Math.random() * 150;
        const close = (open + high + low) / 3 + (Math.random() - 0.5) * 50;
        currentClose = close;

        candles.push({
            time: currentTime,
            open,
            high: Math.max(open, close, high),
            low: Math.min(open, close, low),
            close,
            volume: Math.random() * 20
        });

        ema.push({ time: currentTime, value: close - (Math.random() * 30 - 15) });
        rsi.push({ time: currentTime, value: 30 + Math.random() * 40 });

        currentTime += 15 * 60 * 1000;
    }

    return {
        par: "BTCUSDT",
        intervalo: "15m",
        nomeEstrategia: "Estratégia RSI + EMA",
        lucroTotal: 349.25,
        candles,
        eventos: [
            {
                time: candles[20].time,
                tipo: "COMPRA",
                preco: candles[20].close,
                tooltip: "Ordem de compra executada via Estratégia RSI + EMA",
                cor: "#00E676"
            },
            {
                time: candles[85].time,
                tipo: "VENDA",
                preco: candles[85].close,
                tooltip: "Take Profit atingido (Lucro +1.5%)",
                cor: "#FF5252"
            }
        ],
        indicadores: {
            "RSI_14": rsi,
            "EMA_20": ema,
            "MACD": ema.map(e => ({ ...e, value: e.value * 0.5 })),
            "BOLLINGER_UP": ema.map(e => ({ ...e, value: e.value + 100 })),
            "BOLLINGER_DOWN": ema.map(e => ({ ...e, value: e.value - 100 })),
            "VOLUME_OSC": rsi.map(r => ({ ...r, value: r.value - 20 })),
            "STOCHASTIC": rsi.map(r => ({ ...r, value: r.value + 15 }))
        }
    };
};

const MOCK_DATA = gerarMockData();

const MonitoramentoModal: React.FC<MonitoramentoModalProps> = ({ open, onClose }) => {
    const theme = useTheme();

    const [chartContainer, setChartContainer] = useState<HTMLDivElement | null>(null);
    const chartRef = useRef<IChartApi | null>(null);
    const candleSeriesRef = useRef<any>(null);
    const lineSeriesRef = useRef<Map<string, any>>(new Map());
    const indicatorContainersRef = useRef<{ [key: string]: HTMLDivElement }>({});
    const chartsRef = useRef<{ api: IChartApi, container: HTMLDivElement }[]>([]);

    const [isListOpen, setIsListOpen] = useState(false);
    const [eventFilter, setEventFilter] = useState<'ALL' | 'COMPRA' | 'VENDA'>('ALL');
    const [isIndicatorsExpanded, setIsIndicatorsExpanded] = useState(true);
    const [refreshKey, setRefreshKey] = useState(0);
    const [tooltipData, setTooltipData] = useState<any | null>(null);
    const [alertMessage, setAlertMessage] = useState("");

    // Estado inicial calculado dinamicamente para limitar osciladores e sobrepostos visíveis (max 2 de cada)
    const initialHidden = useMemo(() => {
        const hidden = new Set<string>();
        let oscCount = 0;
        let overlayCount = 0;

        Object.keys(MOCK_DATA.indicadores).forEach(nome => {
            const isOverlay = nome.startsWith('EMA') || nome.startsWith('SMA') || nome.startsWith('BOLLINGER');
            if (isOverlay) {
                overlayCount++;
                if (overlayCount > 2) {
                    hidden.add(nome);
                }
            } else {
                oscCount++;
                if (oscCount > 2) {
                    hidden.add(nome);
                }
            }
        });
        return hidden;
    }, []);

    const [hiddenIndicators, setHiddenIndicators] = useState<Set<string>>(initialHidden);
    const hiddenIndicatorsRef = useRef<Set<string>>(initialHidden);
    
    // Configuração de Ordem (Subir/Descer)
    const initialOscillatorOrder = useMemo(() => {
        return Object.keys(MOCK_DATA.indicadores).filter(n => !(n.startsWith('EMA') || n.startsWith('SMA') || n.startsWith('BOLLINGER')));
    }, []);
    const [oscillatorOrder, setOscillatorOrder] = useState<string[]>(initialOscillatorOrder);
    const oscillatorOrderRef = useRef<string[]>(initialOscillatorOrder);

    const handleRefresh = () => {
        setRefreshKey(prev => prev + 1);
    };

    const toggleIndicatorVisibility = (nome: string) => {
        const isOverlay = nome.startsWith('EMA') || nome.startsWith('SMA') || nome.startsWith('BOLLINGER');

        setHiddenIndicators(prev => {
            const next = new Set(prev);

            if (next.has(nome)) {
                // Exibir - verificar limite
                const typeFilter = (n: string) => isOverlay ? (n.startsWith('EMA') || n.startsWith('SMA') || n.startsWith('BOLLINGER')) : !(n.startsWith('EMA') || n.startsWith('SMA') || n.startsWith('BOLLINGER'));
                const allOfType = Object.keys(MOCK_DATA.indicadores).filter(typeFilter);
                const visibleOfType = allOfType.filter(n => !next.has(n));

                if (visibleOfType.length >= 2) {
                    setAlertMessage(`Só podem ser exibidos 2 indicadores do tipo ${isOverlay ? 'Sobreposto (EMA/SMA/Bollinger)' : 'Oscilador'} simultaneamente.`);
                    return prev; // Cancela a alteração
                }
                next.delete(nome);
            } else {
                // Ocultar - sempre permitido
                next.add(nome);
            }

            hiddenIndicatorsRef.current = next;

            // Atualiza a visibilidade da linha nativamente para os overlays no mainChart
            if (isOverlay) {
                const series = lineSeriesRef.current.get(nome);
                if (series) {
                    series.applyOptions({ visible: !next.has(nome) });
                }
            }

            // Atualiza os eixos de tempo para mostrar apenas o inferior
            updateTimeScaleVisibility(next);

            return next;
        });
    };

    const updateTimeScaleVisibility = (hiddenSet: Set<string>) => {
        if (!chartRef.current) return;

        const allOscillators = oscillatorOrderRef.current;
        const visibleOscillators = allOscillators.filter(n => !hiddenSet.has(n));

        // O Gráfico principal mostra o eixo de datas APENAS se nenhum oscilador estiver visivel
        chartRef.current.timeScale().applyOptions({ visible: visibleOscillators.length === 0 });

        // Osciladores: Apenas o ÚLTIMO oscilador visível da lista mostra o eixo de datas
        visibleOscillators.forEach((oscNome, index) => {
            const isLast = index === visibleOscillators.length - 1;
            const chartData = chartsRef.current.find(c => c.container === indicatorContainersRef.current[oscNome]);
            if (chartData) {
                chartData.api.timeScale().applyOptions({ visible: isLast });
            }
        });
    };

    const handleMoveOscillator = (nome: string, direction: 'up' | 'down') => {
        setOscillatorOrder(prev => {
            const idx = prev.indexOf(nome);
            const next = [...prev];
            if (direction === 'up' && idx > 0) {
                [next[idx - 1], next[idx]] = [next[idx], next[idx - 1]];
            } else if (direction === 'down' && idx < prev.length - 1) {
                [next[idx + 1], next[idx]] = [next[idx], next[idx + 1]];
            } else {
                return prev;
            }
            oscillatorOrderRef.current = next;
            // É preciso atualizar o timeScale pq o último oscilador visível pode ter mudado de posição
            updateTimeScaleVisibility(hiddenIndicatorsRef.current);
            return next;
        });
    };

    // ============================================================================
    // CONSTRUÇÃO DO GRÁFICO
    // ============================================================================
    useEffect(() => {
        if (!open || !chartContainer) return;

        const mainChart = createChart(chartContainer, {
            layout: {
                background: { type: ColorType.Solid, color: theme.palette.background.paper },
                textColor: theme.palette.text.primary,
            },
            grid: {
                vertLines: { color: theme.palette.divider },
                horzLines: { color: theme.palette.divider },
            },
            timeScale: {
                timeVisible: true,
                secondsVisible: false,
            },
            crosshair: {
                mode: CrosshairMode.Normal,
            },
            leftPriceScale: {
                visible: true,
                borderColor: theme.palette.divider,
            },
            rightPriceScale: {
                visible: true,
                borderColor: theme.palette.divider,
                minimumWidth: 60,
            }
        });

        chartRef.current = mainChart;
        lineSeriesRef.current.clear();

        const charts: { api: IChartApi, container: HTMLDivElement }[] = [
            { api: mainChart, container: chartContainer }
        ];

        const candleSeries = mainChart.addSeries(CandlestickSeries, {
            upColor: '#26a69a',
            downColor: '#ef5350',
            borderVisible: false,
            wickUpColor: '#26a69a',
            wickDownColor: '#ef5350',
            priceScaleId: 'right'
        });
        candleSeriesRef.current = candleSeries;

        const formattedCandles: CandlestickData[] = MOCK_DATA.candles.map(c => ({
            time: (c.time / 1000) as Time,
            open: c.open,
            high: c.high,
            low: c.low,
            close: c.close
        }));
        candleSeries.setData(formattedCandles);

        const markers: SeriesMarker<Time>[] = MOCK_DATA.eventos.map(evt => ({
            time: (evt.time / 1000) as Time,
            position: evt.tipo === 'COMPRA' ? 'belowBar' : 'aboveBar',
            color: evt.tipo === 'COMPRA' ? '#FF5252' : '#00E676',
            shape: evt.tipo === 'COMPRA' ? 'arrowUp' : 'arrowDown',
            text: evt.tipo === 'COMPRA' ? 'C' : 'V',
            size: 2,
        }));
        createSeriesMarkers(candleSeries, markers);

        const lineColors = ['#2962FF', '#FF6D00', '#AA00FF'];
        let lineIndexOverlay = 0;
        let lineIndexOscillator = 0;

        Object.entries(MOCK_DATA.indicadores).forEach(([nome, pontos]) => {
            const isOverlay = nome.startsWith('EMA') || nome.startsWith('SMA') || nome.startsWith('BOLLINGER');

            if (isOverlay) {
                const seriesOptions: any = {
                    color: lineColors[lineIndexOverlay % lineColors.length],
                    lineWidth: 2,
                    title: nome,
                    visible: !hiddenIndicatorsRef.current.has(nome),
                    priceScaleId: 'right'
                };
                const lineSeries = mainChart.addSeries(LineSeries, seriesOptions);
                lineSeries.setData(pontos.map(p => ({
                    time: (p.time / 1000) as Time,
                    value: p.value
                })));
                lineSeriesRef.current.set(nome, lineSeries);
                lineIndexOverlay++;
            } else {
                const container = indicatorContainersRef.current[nome];
                if (!container) return;

                const indChart = createChart(container, {
                    layout: {
                        background: { type: ColorType.Solid, color: theme.palette.background.paper },
                        textColor: theme.palette.text.primary,
                    },
                    grid: {
                        vertLines: { color: theme.palette.divider },
                        horzLines: { color: theme.palette.divider },
                    },
                    timeScale: {
                        timeVisible: true,
                        secondsVisible: false,
                    },
                    crosshair: {
                        mode: CrosshairMode.Normal,
                    },
                    leftPriceScale: {
                        visible: true,
                        borderColor: theme.palette.divider,
                    },
                    rightPriceScale: {
                        visible: true,
                        borderColor: theme.palette.divider,
                        minimumWidth: 60,
                    }
                });
                charts.push({ api: indChart, container });

                const lineSeries = indChart.addSeries(LineSeries, {
                    color: lineColors[lineIndexOscillator % lineColors.length],
                    lineWidth: 2,
                    title: nome,
                });

                lineSeries.setData(pontos.map(p => ({
                    time: (p.time / 1000) as Time,
                    value: p.value
                })));

                lineSeriesRef.current.set(nome, lineSeries);
                lineIndexOscillator++;
            }
        });

        chartsRef.current = charts;
        updateTimeScaleVisibility(hiddenIndicatorsRef.current);

        // Sync TimeScales
        let isSyncing = false;
        let lastKnownRange: any = null;

        charts.forEach(c => {
            c.api.timeScale().subscribeVisibleLogicalRangeChange(range => {
                if (!range || isSyncing) return;
                
                // Ignora eventos de range vindos de gráficos ocultos para que não sobrescrevam a posição real
                if (c.container.clientHeight === 0) return;
                
                lastKnownRange = range;
                
                isSyncing = true;
                charts.forEach(other => {
                    if (other.api !== c.api && other.container.clientHeight > 0) {
                        other.api.timeScale().setVisibleLogicalRange(range);
                    }
                });
                isSyncing = false;
            });
        });

        charts.forEach(c => {
            c.api.subscribeCrosshairMove(param => {
                if (
                    param.point === undefined ||
                    !param.time ||
                    param.point.x < 0 ||
                    param.point.y < 0
                ) {
                    setTooltipData(null);
                    return;
                }
                // O crosshair agora será atualizado naturalmente com base no eixo independente de cada gráfico.
                // A sincronização visual entre painéis é feita pela linha de tempo (timeScale) e Tooltip.

                // Somente exibir popup se o mouse estiver ativamente SOBRE o marcador na tela (hoveredObjectId)
                const isHoveringMarker = param.hoveredObjectId !== undefined;

                const timeMs = (param.time as number) * 1000;
                const eventoHover = isHoveringMarker ? MOCK_DATA.eventos.find(e => e.time === timeMs) : undefined;

                if (eventoHover) {
                    const overlaysTexto: string[] = [];
                    const oscillatorsTexto: string[] = [];

                    Object.entries(MOCK_DATA.indicadores)
                        .filter(([key]) => !hiddenIndicatorsRef.current.has(key))
                        .forEach(([key, pontos]) => {
                            const ponto = pontos.find(p => p.time === timeMs);
                            if (ponto) {
                                const text = `${key}: ${ponto.value.toFixed(2)}`;
                                const isOverlay = key.startsWith('EMA') || key.startsWith('SMA') || key.startsWith('BOLLINGER');
                                if (isOverlay) overlaysTexto.push(text);
                                else oscillatorsTexto.push(text);
                            }
                        });

                    const tooltipWidth = 200;
                    const tooltipHeight = 120 + (overlaysTexto.length + oscillatorsTexto.length) * 15;

                    const parentBox = chartContainer.parentElement;
                    const offsetY = c.container.offsetTop - (parentBox?.offsetTop ?? 0);

                    // Posicionar flutuando acima do marcador, em vez da posição do mouse
                    let renderX = param.point.x - (tooltipWidth / 2);
                    let renderY = param.point.y + offsetY - tooltipHeight - 20;

                    if (parentBox) {
                        if (renderX < 0) renderX = 10;
                        if (renderY < 0) renderY = param.point.y + offsetY + 30; // inverter pra baixo se topo cortar
                    }

                    setTooltipData({
                        x: renderX,
                        y: renderY,
                        tipo: eventoHover.tipo,
                        preco: eventoHover.preco,
                        tooltip: eventoHover.tooltip,
                        corTitulo: eventoHover.tipo === 'COMPRA' ? '#FF5252' : '#00E676',
                        overlays: overlaysTexto,
                        oscillators: oscillatorsTexto
                    });
                } else {
                    setTooltipData(null);
                }
            });
        });

        const ro = new ResizeObserver(entries => {
            for (const entry of entries) {
                const chartData = charts.find(data => data.container === entry.target);
                if (!chartData) continue;

                const w = entry.contentRect.width;
                const h = entry.contentRect.height;
                
                // Se o gráfico estiver visível e tem dimensões
                if (w > 0 && h > 0) {
                    const currentHeight = chartData.api.options().height;
                    const wasHidden = currentHeight === 0;

                    if (chartData.api.options().width !== w || currentHeight !== h) {
                        chartData.api.applyOptions({ width: w, height: h });
                    }

                    // Se esse gráfico acabou de acordar de um display: none/height: 0, 
                    // ele precisa alcançar a câmera do usuário antes de piscar na tela
                    if (wasHidden && lastKnownRange) {
                        chartData.api.timeScale().setVisibleLogicalRange(lastKnownRange);
                    }
                } else if (h === 0) {
                    // O gráfico foi ocultado pelo usuário.
                    // Forçamos a constraint dele para 0 internamente para trackear quando ressucitar
                    const currentHeight = chartData.api.options().height;
                    if (currentHeight !== 0) {
                        chartData.api.applyOptions({ height: 0 });
                    }
                }
            }
        });
        charts.forEach(c => ro.observe(c.container));

        setTimeout(() => {
            charts.forEach(c => {
                if (c.container.clientWidth > 0 && c.container.clientHeight > 0) {
                    c.api.applyOptions({ width: c.container.clientWidth, height: c.container.clientHeight });
                }
            });

            if (mainChart) mainChart.timeScale().fitContent();
        }, 100);

        return () => {
            ro.disconnect();
            charts.forEach(c => c.api.remove());
            chartRef.current = null;
            candleSeriesRef.current = null;
            chartsRef.current = [];
        };
    }, [open, theme, refreshKey, chartContainer]);

    // ============================================================================
    // INTERAÇÃO: Animação e Centralização ao Clicar na Lista
    // ============================================================================
    const handleEventClick = (eventoTimeMs: number) => {
        if (!chartRef.current || !candleSeriesRef.current) return;

        const candleIndex = MOCK_DATA.candles.findIndex(c => c.time === eventoTimeMs);
        if (candleIndex === -1) return;

        chartRef.current.timeScale().setVisibleLogicalRange({
            from: candleIndex - 10,
            to: candleIndex + 10
        });

        const highlightedData = MOCK_DATA.candles.map(c => {
            const baseFormat = {
                time: (c.time / 1000) as Time,
                open: c.open,
                high: c.high,
                low: c.low,
                close: c.close,
            };
            if (c.time === eventoTimeMs) {
                return { ...baseFormat, color: '#FFFF00', borderColor: '#FFFF00', wickColor: '#FFFF00' };
            }
            return baseFormat;
        });

        candleSeriesRef.current.setData(highlightedData);

        setTimeout(() => {
            if (candleSeriesRef.current) {
                const originalData = MOCK_DATA.candles.map(c => ({
                    time: (c.time / 1000) as Time,
                    open: c.open,
                    high: c.high,
                    low: c.low,
                    close: c.close,
                }));
                candleSeriesRef.current.setData(originalData);
            }
        }, 1000);
    };

    const qtdCompras = MOCK_DATA.eventos.filter(e => e.tipo === 'COMPRA').length;
    const qtdVendas = MOCK_DATA.eventos.filter(e => e.tipo === 'VENDA').length;

    const isLightMode = theme.palette.mode === 'light';

    return (
        <Dialog
            open={open}
            onClose={onClose}
            maxWidth="xl"
            fullWidth
            PaperProps={{ sx: { height: '90vh', display: 'flex', flexDirection: 'column' } }}
        >
            <DialogTitle sx={{ textAlign: 'center', fontWeight: 'bold', position: 'relative', borderBottom: 1, borderColor: 'divider' }}>
                Monitoramento {MOCK_DATA.par} {MOCK_DATA.intervalo} - {MOCK_DATA.nomeEstrategia}

                <Box sx={{ position: 'absolute', right: 16, top: '50%', transform: 'translateY(-50%)', display: 'flex', gap: 1 }}>
                    <Button
                        startIcon={<RefreshIcon />}
                        variant="outlined"
                        size="small"
                        onClick={handleRefresh}
                    >
                        Atualizar
                    </Button>
                    <IconButton onClick={onClose} size="small"><CloseIcon /></IconButton>
                </Box>
            </DialogTitle>

            <DialogContent sx={{ p: 0, display: 'flex', flexDirection: 'column', flex: 1, overflow: 'hidden', backgroundColor: theme.palette.background.default }}>
                <Box sx={{ display: 'flex', flex: 1, minHeight: 0 }}>

                    <Box sx={{ flexGrow: 1, position: 'relative', m: 2, borderRadius: 2, border: `1px solid ${theme.palette.divider}` }}>

                        {/* Menu HUD Flutuante de Indicadores com Isolamento de Z-Index e Supressão de Eventos */}
                        <Box
                            onPointerDown={(e) => e.stopPropagation()}
                            onClick={(e) => e.stopPropagation()}
                            sx={{ position: 'absolute', top: 10, left: 10, zIndex: 5, display: 'flex', flexDirection: 'column', gap: 0.5 }}
                        >
                            <Box
                                onClick={() => setIsIndicatorsExpanded(!isIndicatorsExpanded)}
                                sx={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    bgcolor: theme.palette.background.paper,
                                    border: `1px solid ${theme.palette.divider}`,
                                    opacity: 0.9,
                                    borderRadius: 1,
                                    px: 1,
                                    py: 0.5,
                                    cursor: 'pointer',
                                    '&:hover': { opacity: 1 },
                                    boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
                                }}
                            >
                                <LayersIcon sx={{ fontSize: 16, mr: 1, color: 'primary.main' }} />
                                <Typography variant="caption" sx={{ fontWeight: 'bold' }}>Indicadores Ativos</Typography>
                                {isIndicatorsExpanded ? <KeyboardArrowUp sx={{ fontSize: 16, ml: 1 }} /> : <KeyboardArrowDown sx={{ fontSize: 16, ml: 1 }} />}
                            </Box>

                            {isIndicatorsExpanded && (() => {
                                const allNames = Object.keys(MOCK_DATA.indicadores);
                                const overlays = allNames.filter(nome => nome.startsWith('EMA') || nome.startsWith('SMA') || nome.startsWith('BOLLINGER'));
                                const oscillators = allNames.filter(nome => !nome.startsWith('EMA') && !nome.startsWith('SMA') && !nome.startsWith('BOLLINGER'));

                                const renderItem = (nome: string, isOverlay: boolean) => {
                                    const isHidden = hiddenIndicators.has(nome);
                                    return (
                                        <Box key={nome} sx={{ display: 'flex', alignItems: 'center', gap: 0.5, pl: 1 }}>
                                            <Tooltip title={isHidden ? "Exibir no gráfico" : "Ocultar do gráfico"} placement="right">
                                                <Box
                                                    onClick={() => toggleIndicatorVisibility(nome)}
                                                    sx={{
                                                        display: 'flex',
                                                        alignItems: 'center',
                                                        justifyContent: 'space-between',
                                                        bgcolor: theme.palette.background.paper,
                                                        border: `1px solid ${theme.palette.divider}`,
                                                        opacity: 0.85,
                                                        borderRadius: 1,
                                                        px: 1,
                                                        py: 0.5,
                                                        cursor: 'pointer',
                                                        '&:hover': { opacity: 1 },
                                                        width: 140
                                                    }}
                                                >
                                                    <Typography variant="caption" sx={{ fontWeight: 'bold', color: isHidden ? 'text.disabled' : (isOverlay ? 'primary.main' : 'secondary.main'), overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                                        {nome}
                                                    </Typography>
                                                    {isHidden ? <VisibilityOff sx={{ fontSize: 16, color: 'text.disabled', ml: 1 }} /> : <Visibility sx={{ fontSize: 16, color: 'text.primary', ml: 1 }} />}
                                                </Box>
                                            </Tooltip>

                                            {!isOverlay && !isHidden && (
                                                <Box sx={{ display: 'flex', flexDirection: 'column' }}>
                                                    <KeyboardArrowUp 
                                                        onClick={(e) => { e.stopPropagation(); handleMoveOscillator(nome, 'up'); }} 
                                                        sx={{ fontSize: 16, cursor: 'pointer', color: 'text.secondary', '&:hover': { color: 'primary.main' } }} 
                                                    />
                                                    <KeyboardArrowDown 
                                                        onClick={(e) => { e.stopPropagation(); handleMoveOscillator(nome, 'down'); }} 
                                                        sx={{ fontSize: 16, cursor: 'pointer', color: 'text.secondary', '&:hover': { color: 'primary.main' } }} 
                                                    />
                                                </Box>
                                            )}
                                        </Box>
                                    );
                                };

                                return (
                                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
                                        {overlays.length > 0 && (
                                            <>
                                                <Typography variant="caption" sx={{ fontWeight: 'bold', color: 'text.secondary', pl: 0.5, mt: 0.5 }}>Sobrepostos (Gráfico)</Typography>
                                                {overlays.map(nome => renderItem(nome, true))}
                                            </>
                                        )}
                                        {oscillators.length > 0 && (
                                            <>
                                                <Typography variant="caption" sx={{ fontWeight: 'bold', color: 'text.secondary', pl: 0.5, mt: 0.5 }}>Osciladores (Inferiores)</Typography>
                                                {oscillators.map(nome => renderItem(nome, false))}
                                            </>
                                        )}
                                    </Box>
                                );
                            })()}
                        </Box>

                        {/* Menu HUD Flutuante do Histórico de Eventos */}
                        <Box
                            onPointerDown={(e) => e.stopPropagation()}
                            onClick={(e) => e.stopPropagation()}
                            sx={{ position: 'absolute', top: 10, left: 170, zIndex: 6, display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 1 }}
                        >
                            <Box
                                onClick={() => setIsListOpen(!isListOpen)}
                                sx={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    bgcolor: theme.palette.background.paper,
                                    border: `1px solid ${theme.palette.divider}`,
                                    opacity: 0.9,
                                    borderRadius: 1,
                                    px: 1,
                                    py: 0.5,
                                    cursor: 'pointer',
                                    '&:hover': { opacity: 1 },
                                    boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
                                }}
                            >
                                <FormatListBulleted sx={{ fontSize: 16, mr: 1, color: 'success.main' }} />
                                <Typography variant="caption" sx={{ fontWeight: 'bold' }}>Histórico de Eventos</Typography>
                            </Box>

                            {isListOpen && (
                                <Paper 
                                    sx={{ 
                                        width: 300, 
                                        maxHeight: 'calc(100vh - 200px)', 
                                        overflowY: 'auto', 
                                        border: `1px solid ${theme.palette.divider}`,
                                        boxShadow: '0 8px 32px rgba(0,0,0,0.2)',
                                        bgcolor: isLightMode ? 'rgba(255, 255, 255, 0.95)' : 'rgba(30, 34, 45, 0.95)',
                                        backdropFilter: 'blur(4px)'
                                    }}
                                >
                                    <Box sx={{ p: 1, borderBottom: `1px solid ${theme.palette.divider}`, display: 'flex', justifyContent: 'center' }}>
                                        <ToggleButtonGroup
                                            size="small"
                                            value={eventFilter}
                                            exclusive
                                            onChange={(_, newValue) => { if(newValue) setEventFilter(newValue); }}
                                            aria-label="Filtro de Eventos"
                                            sx={{ '& .MuiToggleButton-root': { py: 0.2, px: 1, fontSize: '0.7rem' } }}
                                        >
                                            <ToggleButton value="ALL" aria-label="Todos">Todos</ToggleButton>
                                            <ToggleButton value="COMPRA" aria-label="Compras" sx={{ color: 'error.main', '&.Mui-selected': { bgcolor: 'rgba(255, 82, 82, 0.1)', color: 'error.main' } }}>Compras</ToggleButton>
                                            <ToggleButton value="VENDA" aria-label="Vendas" sx={{ color: 'success.main', '&.Mui-selected': { bgcolor: 'rgba(0, 230, 118, 0.1)', color: 'success.main' } }}>Vendas</ToggleButton>
                                        </ToggleButtonGroup>
                                    </Box>
                                    <List disablePadding>
                                        {MOCK_DATA.eventos.filter(e => eventFilter === 'ALL' || e.tipo === eventFilter).map((evento, idx) => (
                                            <React.Fragment key={idx}>
                                                <ListItem disablePadding>
                                                    <ListItemButton onClick={() => handleEventClick(evento.time)}>
                                                        <ListItemText
                                                            primary={
                                                                <Typography variant="body2" fontWeight="bold" color={evento.tipo === 'COMPRA' ? 'error.main' : 'success.main'}>
                                                                    {evento.tipo} - ${evento.preco.toFixed(2)}
                                                                </Typography>
                                                            }
                                                            secondary={
                                                                <>
                                                                    <Typography variant="caption" display="block">
                                                                        {new Date(evento.time).toLocaleString()}
                                                                    </Typography>
                                                                    <Typography variant="caption" color="text.secondary">
                                                                        {evento.tooltip}
                                                                    </Typography>
                                                                </>
                                                            }
                                                        />
                                                    </ListItemButton>
                                                </ListItem>
                                                <Divider />
                                            </React.Fragment>
                                        ))}
                                    </List>
                                </Paper>
                            )}
                        </Box>

                        <div style={{ display: 'flex', flexDirection: 'column', width: '100%', height: '100%', overflowY: 'hidden' }}>
                            <div ref={setChartContainer} style={{ flexGrow: 1, minHeight: 0 }} />
                            {oscillatorOrder.map((nome, index) => {
                                return (
                                    <div
                                        key={nome}
                                        ref={el => { if (el) indicatorContainersRef.current[nome] = el; }}
                                        style={{
                                            height: hiddenIndicators.has(nome) ? 0 : 150,
                                            display: hiddenIndicators.has(nome) ? 'none' : 'block',
                                            borderTop: hiddenIndicators.has(nome) ? 'none' : `1px solid ${theme.palette.divider}`,
                                            flexShrink: 0,
                                            order: index
                                        }}
                                    />
                                );
                            })}
                        </div>

                        {tooltipData && (
                            <Paper
                                elevation={12}
                                sx={{
                                    position: 'absolute',
                                    left: tooltipData.x,
                                    top: tooltipData.y,
                                    zIndex: 100,
                                    p: 1.5,
                                    minWidth: 160,
                                    pointerEvents: 'none',
                                    backgroundColor: isLightMode ? 'rgba(255, 255, 255, 0.95)' : 'rgba(30, 34, 45, 0.95)',
                                    color: theme.palette.text.primary,
                                    borderRadius: 1,
                                    border: `1px solid ${theme.palette.divider}`,
                                    backdropFilter: 'blur(4px)',
                                    boxShadow: '0 8px 32px rgba(0,0,0,0.2)'
                                }}
                            >
                                <Typography variant="caption" sx={{ color: tooltipData.corTitulo, fontWeight: 'bold', textTransform: 'uppercase', display: 'flex', alignItems: 'center', gap: 0.5 }}>
                                    <span style={{ width: 8, height: 8, borderRadius: '50%', backgroundColor: tooltipData.corTitulo }}></span>
                                    {tooltipData.tipo} @ {tooltipData.preco.toFixed(2)}
                                </Typography>
                                <Typography variant="caption" display="block" sx={{ mt: 0.5, mb: 1.5, opacity: 0.8, lineHeight: 1.2 }}>
                                    {tooltipData.tooltip}
                                </Typography>

                                {(tooltipData.overlays?.length > 0 || tooltipData.oscillators?.length > 0) && (
                                    <Box sx={{ borderTop: `1px dashed ${theme.palette.divider}`, pt: 1 }}>
                                        {tooltipData.overlays?.length > 0 && (
                                            <Box sx={{ mb: tooltipData.oscillators?.length > 0 ? 1 : 0 }}>
                                                {tooltipData.overlays.map((ind: string, idx: number) => (
                                                    <Typography key={`ov-${idx}`} variant="caption" display="flex" justifyContent="space-between" sx={{ fontFamily: 'monospace', fontSize: '0.7rem' }}>
                                                        <span style={{ opacity: 0.6 }}>{ind.split(':')[0]}</span>
                                                        <span>{ind.split(':')[1]}</span>
                                                    </Typography>
                                                ))}
                                            </Box>
                                        )}
                                        {tooltipData.oscillators?.length > 0 && (
                                            <Box>
                                                {tooltipData.oscillators.map((ind: string, idx: number) => (
                                                    <Typography key={`os-${idx}`} variant="caption" display="flex" justifyContent="space-between" sx={{ fontFamily: 'monospace', fontSize: '0.7rem' }}>
                                                        <span style={{ opacity: 0.6 }}>{ind.split(':')[0]}</span>
                                                        <span>{ind.split(':')[1]}</span>
                                                    </Typography>
                                                ))}
                                            </Box>
                                        )}
                                    </Box>
                                )}
                            </Paper>
                        )}
                    </Box>
                </Box>

                <Box sx={{ p: 2, borderTop: `1px solid ${theme.palette.divider}`, bgcolor: theme.palette.background.paper }}>
                    <Grid container spacing={3} justifyContent="center">
                        <Grid item xs={12} sm={4}>
                            <Paper variant="outlined" sx={{ p: 2, display: 'flex', alignItems: 'center', gap: 2, bgcolor: 'rgba(0, 230, 118, 0.05)' }}>
                                <TrendingUp color="success" fontSize="large" />
                                <Box>
                                    <Typography variant="caption" color="text.secondary" textTransform="uppercase">Lucro Total</Typography>
                                    <Typography variant="h5" fontWeight="bold" color="success.main">
                                        + ${MOCK_DATA.lucroTotal.toFixed(2)}
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
                                        {qtdCompras}
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
                                        {qtdVendas}
                                    </Typography>
                                </Box>
                            </Paper>
                        </Grid>
                    </Grid>
                </Box>
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