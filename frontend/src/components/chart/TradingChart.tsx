import React, { useState, useEffect, useRef, useMemo } from 'react';
import {
    Box,
    Typography,
    useTheme,
    Paper,
    List,
    ListItem,
    ListItemText,
    ListItemButton,
    Divider,
    Tooltip,
    ToggleButton,
    ToggleButtonGroup,
    Snackbar,
    Alert
} from '@mui/material';
import {
    FormatListBulleted,
    Layers as LayersIcon,
    KeyboardArrowDown,
    KeyboardArrowUp,
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
import type { MonitoramentoDataDTO } from '../../types/monitoramento';

export interface TradingChartProps {
    data: MonitoramentoDataDTO;
}

const TradingChart: React.FC<TradingChartProps> = ({ data }) => {
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
    const [tooltipData, setTooltipData] = useState<any | null>(null);
    const [alertMessage, setAlertMessage] = useState("");

    const initialHidden = useMemo(() => {
        const hidden = new Set<string>();
        if (!data) return hidden;

        let oscCount = 0;
        let overlayCount = 0;

        Object.keys(data.indicadores).forEach(nome => {
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
    }, [data]);

    const initialOscillatorOrder = useMemo(() => {
        if (!data) return [];
        return Object.keys(data.indicadores).filter(n => !(n.startsWith('EMA') || n.startsWith('SMA') || n.startsWith('BOLLINGER')));
    }, [data]);

    const [hiddenIndicators, setHiddenIndicators] = useState<Set<string>>(initialHidden);
    const hiddenIndicatorsRef = useRef<Set<string>>(initialHidden);

    const [oscillatorOrder, setOscillatorOrder] = useState<string[]>(initialOscillatorOrder);
    const oscillatorOrderRef = useRef<string[]>(initialOscillatorOrder);

    useEffect(() => {
        setHiddenIndicators(initialHidden);
        hiddenIndicatorsRef.current = initialHidden;
        setOscillatorOrder(initialOscillatorOrder);
        oscillatorOrderRef.current = initialOscillatorOrder;
    }, [initialHidden, initialOscillatorOrder]);

    const updateTimeScaleVisibility = (hiddenSet: Set<string>) => {
        if (!chartRef.current) return;

        const allOscillators = oscillatorOrderRef.current;
        const visibleOscillators = allOscillators.filter(n => !hiddenSet.has(n));

        chartRef.current.timeScale().applyOptions({ visible: visibleOscillators.length === 0 });

        visibleOscillators.forEach((oscNome, index) => {
            const isLast = index === visibleOscillators.length - 1;
            const chartData = chartsRef.current.find(c => c.container === indicatorContainersRef.current[oscNome]);
            if (chartData) {
                chartData.api.timeScale().applyOptions({ visible: isLast });
            }
        });
    };

    const toggleIndicatorVisibility = (nome: string) => {
        if (!data) return;
        const isOverlay = nome.startsWith('EMA') || nome.startsWith('SMA') || nome.startsWith('BOLLINGER');

        setHiddenIndicators(prev => {
            const next = new Set(prev);

            if (next.has(nome)) {
                const typeFilter = (n: string) => isOverlay ? (n.startsWith('EMA') || n.startsWith('SMA') || n.startsWith('BOLLINGER')) : !(n.startsWith('EMA') || n.startsWith('SMA') || n.startsWith('BOLLINGER'));
                const allOfType = Object.keys(data.indicadores).filter(typeFilter);
                const visibleOfType = allOfType.filter(n => !next.has(n));

                if (visibleOfType.length >= 2) {
                    setAlertMessage(`Só podem ser exibidos 2 indicadores do tipo ${isOverlay ? 'Sobreposto' : 'Oscilador'} simultaneamente.`);
                    return prev;
                }
                next.delete(nome);
            } else {
                next.add(nome);
            }

            hiddenIndicatorsRef.current = next;

            if (isOverlay) {
                const series = lineSeriesRef.current.get(nome);
                if (series) {
                    series.applyOptions({ visible: !next.has(nome) });
                }
            }

            updateTimeScaleVisibility(next);
            return next;
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
            updateTimeScaleVisibility(hiddenIndicatorsRef.current);
            return next;
        });
    };

    useEffect(() => {
        if (!chartContainer || !data) return;

        const mainChart = createChart(chartContainer, {
            layout: {
                background: { type: ColorType.Solid, color: theme.palette.background.paper },
                textColor: theme.palette.text.primary,
                attributionLogo: false
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

        const formattedCandles: CandlestickData[] = data.candles.map(c => ({
            time: c.time as Time,
            open: c.open,
            high: c.high,
            low: c.low,
            close: c.close
        }));

        formattedCandles.sort((a, b) => (a.time as number) - (b.time as number));
        if (formattedCandles.length > 0) {
            candleSeries.setData(formattedCandles);
        }

        const markers: SeriesMarker<Time>[] = data.eventos.map(evt => ({
            time: evt.time as Time,
            position: evt.tipo === 'COMPRA' ? 'belowBar' : 'aboveBar',
            color: evt.cor || (evt.tipo === 'COMPRA' ? '#26a69a' : '#ef5350'),
            shape: evt.tipo === 'COMPRA' ? 'arrowUp' : 'arrowDown',
            text: evt.tipo === 'COMPRA' ? 'C' : 'V',
            size: 2,
        }));
        markers.sort((a, b) => (a.time as number) - (b.time as number));
        if (markers.length > 0) {
            createSeriesMarkers(candleSeries, markers);
        }

        const lineColors = ['#2962FF', '#FF6D00', '#AA00FF'];
        let lineIndexOverlay = 0;
        let lineIndexOscillator = 0;

        Object.entries(data.indicadores).forEach(([nome, pontos]) => {
            const isOverlay = nome.startsWith('EMA') || nome.startsWith('SMA') || nome.startsWith('BOLLINGER');

            const pontosOrdenados = [...pontos].sort((a, b) => a.time - b.time);

            if (isOverlay) {
                const seriesOptions: any = {
                    color: lineColors[lineIndexOverlay % lineColors.length],
                    lineWidth: 2,
                    title: nome,
                    visible: !hiddenIndicatorsRef.current.has(nome),
                    priceScaleId: 'right'
                };
                const lineSeries = mainChart.addSeries(LineSeries, seriesOptions);
                if (pontosOrdenados.length > 0) {
                    lineSeries.setData(pontosOrdenados.map(p => ({
                        time: p.time as Time,
                        value: p.value
                    })));
                }
                lineSeriesRef.current.set(nome, lineSeries);
                lineIndexOverlay++;
            } else {
                const container = indicatorContainersRef.current[nome];
                if (!container) return;

                const indChart = createChart(container, {
                    layout: {
                        background: { type: ColorType.Solid, color: theme.palette.background.paper },
                        textColor: theme.palette.text.primary,
                        attributionLogo: false
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

                if (pontosOrdenados.length > 0) {
                    lineSeries.setData(pontosOrdenados.map(p => ({
                        time: p.time as Time,
                        value: p.value
                    })));
                }

                lineSeriesRef.current.set(nome, lineSeries);
                lineIndexOscillator++;
            }
        });

        chartsRef.current = charts;
        updateTimeScaleVisibility(hiddenIndicatorsRef.current);

        let isSyncing = false;
        let lastKnownRange: any = null;

        charts.forEach(c => {
            c.api.timeScale().subscribeVisibleLogicalRangeChange(range => {
                if (!range || isSyncing) return;
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
                if (param.point === undefined || !param.time || param.point.x < 0 || param.point.y < 0) {
                    setTooltipData(null);
                    return;
                }

                const isHoveringMarker = param.hoveredObjectId !== undefined;
                const timeS = param.time as number;
                const eventoHover = isHoveringMarker ? data.eventos.find(e => e.time === timeS) : undefined;

                if (eventoHover) {
                    const overlaysTexto: string[] = [];
                    const oscillatorsTexto: string[] = [];

                    Object.entries(data.indicadores)
                        .filter(([key]) => !hiddenIndicatorsRef.current.has(key))
                        .forEach(([key, pontos]) => {
                            const ponto = pontos.find(p => p.time === timeS);
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

                    let renderX = param.point.x - (tooltipWidth / 2);
                    let renderY = param.point.y + offsetY - tooltipHeight - 20;

                    if (parentBox) {
                        if (renderX < 0) renderX = 10;
                        if (renderY < 0) renderY = param.point.y + offsetY + 30;
                    }

                    setTooltipData({
                        x: renderX,
                        y: renderY,
                        tipo: eventoHover.tipo,
                        preco: eventoHover.preco,
                        tooltip: eventoHover.tooltip,
                        corTitulo: eventoHover.cor || (eventoHover.tipo === 'COMPRA' ? '#26a69a' : '#ef5350'),
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
                const chartData = charts.find(c => c.container === entry.target);
                if (!chartData) continue;

                const w = entry.contentRect.width;
                const h = entry.contentRect.height;

                if (w > 0 && h > 0) {
                    const currentHeight = chartData.api.options().height;
                    const wasHidden = currentHeight === 0;

                    if (chartData.api.options().width !== w || currentHeight !== h) {
                        chartData.api.applyOptions({ width: w, height: h });
                    }

                    if (wasHidden && lastKnownRange) {
                        chartData.api.timeScale().setVisibleLogicalRange(lastKnownRange);
                    }
                } else if (h === 0) {
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
            if (mainChart && formattedCandles.length > 0) {
                mainChart.timeScale().fitContent();
            }
        }, 100);

        return () => {
            ro.disconnect();
            charts.forEach(c => c.api.remove());
            chartRef.current = null;
            candleSeriesRef.current = null;
            chartsRef.current = [];
        };
    }, [data, theme, chartContainer]);

    const handleEventClick = (eventoTimeS: number) => {
        if (!chartRef.current || !candleSeriesRef.current || !data) return;

        const candleIndex = data.candles.findIndex(c => c.time === eventoTimeS);
        if (candleIndex === -1) return;

        chartRef.current.timeScale().setVisibleLogicalRange({
            from: candleIndex - 10,
            to: candleIndex + 10
        });

        const highlightedData = data.candles.map(c => {
            const baseFormat = {
                time: c.time as Time,
                open: c.open,
                high: c.high,
                low: c.low,
                close: c.close,
            };
            if (c.time === eventoTimeS) {
                return { ...baseFormat, color: '#FFFF00', borderColor: '#FFFF00', wickColor: '#FFFF00' };
            }
            return baseFormat;
        });

        candleSeriesRef.current.setData(highlightedData);

        setTimeout(() => {
            if (candleSeriesRef.current) {
                const originalData = data.candles.map(c => ({
                    time: c.time as Time,
                    open: c.open,
                    high: c.high,
                    low: c.low,
                    close: c.close,
                }));
                candleSeriesRef.current.setData(originalData);
            }
        }, 1000);
    };

    const isLightMode = theme.palette.mode === 'light';

    return (
        <Box sx={{ flexGrow: 1, position: 'relative', m: 2, borderRadius: 2, border: `1px solid ${theme.palette.divider}` }}>
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
                    const allNames = Object.keys(data.indicadores);
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
                                onChange={(_, newValue) => { if (newValue) setEventFilter(newValue); }}
                                aria-label="Filtro de Eventos"
                                sx={{ '& .MuiToggleButton-root': { py: 0.2, px: 1, fontSize: '0.7rem' } }}
                            >
                                <ToggleButton value="ALL" aria-label="Todos">Todos</ToggleButton>
                                <ToggleButton value="COMPRA" aria-label="Compras" sx={{ color: 'error.main', '&.Mui-selected': { bgcolor: 'rgba(255, 82, 82, 0.1)', color: 'error.main' } }}>Compras</ToggleButton>
                                <ToggleButton value="VENDA" aria-label="Vendas" sx={{ color: 'success.main', '&.Mui-selected': { bgcolor: 'rgba(0, 230, 118, 0.1)', color: 'success.main' } }}>Vendas</ToggleButton>
                            </ToggleButtonGroup>
                        </Box>
                        <List disablePadding>
                            {data.eventos.filter(e => eventFilter === 'ALL' || e.tipo === eventFilter).map((evento, idx) => (
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
                                                            {new Date(evento.time * 1000).toLocaleString()}
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
                            {data.eventos.length === 0 && (
                                <ListItem>
                                    <ListItemText secondary="Nenhum evento registrado ainda." />
                                </ListItem>
                            )}
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
        </Box>
    );
};

export default TradingChart;