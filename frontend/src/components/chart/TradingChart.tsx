import React, { useState, useEffect, useRef, useMemo, useCallback } from 'react';
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

interface HoveredData {
    time: number;
    open: number;
    high: number;
    low: number;
    close: number;
    volume: number;
    indicators: { [key: string]: number };
}

interface ChartItem {
    api: IChartApi;
    container: HTMLDivElement;
    nome: string;
}

const LINE_COLORS = ['#2962FF', '#FF6D00', '#AA00FF', '#00BCD4', '#FF5252', '#4CAF50'];

const TradingChart: React.FC<TradingChartProps> = ({ data }) => {
    const theme = useTheme();

    const [chartContainer, setChartContainer] = useState<HTMLDivElement | null>(null);
    const chartRef = useRef<IChartApi | null>(null);
    const candleSeriesRef = useRef<any>(null);
    const lineSeriesRef = useRef<Map<string, any>>(new Map());
    const indicatorContainersRef = useRef<{ [key: string]: HTMLDivElement }>({});
    const chartsRef = useRef<ChartItem[]>([]);

    const [isListOpen, setIsListOpen] = useState(false);
    const [eventFilter, setEventFilter] = useState<'ALL' | 'COMPRA' | 'VENDA'>('ALL');
    const [isIndicatorsExpanded, setIsIndicatorsExpanded] = useState(true);
    const [tooltipData, setTooltipData] = useState<any | null>(null);
    const [alertMessage, setAlertMessage] = useState("");
    const [hoveredCandle, setHoveredCandle] = useState<HoveredData | null>(null);

    // Identifica apenas indicadores que realmente possuem dados válidos (> 0)
    const validIndicators = useMemo(() => {
        if (!data?.indicadores) return {};
        const filtered: { [key: string]: typeof data.indicadores[string] } = {};
        Object.entries(data.indicadores).forEach(([nome, pontos]) => {
            const hasValidPoints = pontos && pontos.some(p => p.value != null && Number(p.value) > 0);
            if (hasValidPoints) {
                filtered[nome] = pontos;
            }
        });
        return filtered;
    }, [data?.indicadores]);

    const initialHidden = useMemo(() => {
        const hidden = new Set<string>();
        if (!validIndicators) return hidden;

        let oscCount = 0;
        let overlayCount = 0;

        Object.keys(validIndicators).forEach(nome => {
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
    }, [validIndicators]);

    const initialOscillatorOrder = useMemo(() => {
        if (!validIndicators) return [];
        return Object.keys(validIndicators).filter(n => !(n.startsWith('EMA') || n.startsWith('SMA') || n.startsWith('BOLLINGER')));
    }, [validIndicators]);

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

    const updateTimeScaleVisibility = useCallback((hiddenSet: Set<string>) => {
        if (!chartRef.current) return;

        const allOscillators = oscillatorOrderRef.current;
        const visibleOscillators = allOscillators.filter(n => !hiddenSet.has(n));

        // Gráfico principal SEMPRE exibe escala de tempo
        chartRef.current.timeScale().applyOptions({ visible: true });

        // O último oscilador visível no rodapé também exibe a régua de tempo
        visibleOscillators.forEach((oscNome, index) => {
            const isLast = index === visibleOscillators.length - 1;
            const chartData = chartsRef.current.find(c => c.container === indicatorContainersRef.current[oscNome]);
            if (chartData) {
                chartData.api.timeScale().applyOptions({ visible: isLast });
            }
        });
    }, []);

    const toggleIndicatorVisibility = (nome: string) => {
        if (!validIndicators) return;
        const isOverlay = nome.startsWith('EMA') || nome.startsWith('SMA') || nome.startsWith('BOLLINGER');

        setHiddenIndicators(prev => {
            const next = new Set(prev);

            if (next.has(nome)) {
                const typeFilter = (n: string) => isOverlay ? (n.startsWith('EMA') || n.startsWith('SMA') || n.startsWith('BOLLINGER')) : !(n.startsWith('EMA') || n.startsWith('SMA') || n.startsWith('BOLLINGER'));
                const allOfType = Object.keys(validIndicators).filter(typeFilter);
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

    // Dados do último candle para exibição padrão quando não houver hover
    const latestCandleInfo = useMemo<HoveredData | null>(() => {
        if (!data?.candles || data.candles.length === 0) return null;
        const lastCandle = data.candles[data.candles.length - 1];
        const inds: { [key: string]: number } = {};
        Object.entries(validIndicators).forEach(([k, pts]) => {
            if (!hiddenIndicators.has(k)) {
                const pt = pts.find(p => p.time === lastCandle.time);
                if (pt && Number(pt.value) > 0) {
                    inds[k] = pt.value;
                }
            }
        });
        return {
            time: lastCandle.time,
            open: lastCandle.open,
            high: lastCandle.high,
            low: lastCandle.low,
            close: lastCandle.close,
            volume: lastCandle.volume,
            indicators: inds
        };
    }, [data?.candles, validIndicators, hiddenIndicators]);

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
                rightOffset: 12,
                barSpacing: 9,
                minBarSpacing: 3,
                fixLeftEdge: false,
                fixRightEdge: false,
                borderColor: theme.palette.divider,
            },
            crosshair: {
                mode: CrosshairMode.Normal,
            },
            leftPriceScale: {
                visible: false,
            },
            rightPriceScale: {
                visible: true,
                borderColor: theme.palette.divider,
                minimumWidth: 70,
                autoScale: true,
            }
        });

        chartRef.current = mainChart;
        lineSeriesRef.current.clear();

        const charts: ChartItem[] = [
            { api: mainChart, container: chartContainer, nome: 'MAIN' }
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

        let lineIndexOverlay = 0;
        let lineIndexOscillator = 0;

        Object.entries(validIndicators).forEach(([nome, pontos]) => {
            const isOverlay = nome.startsWith('EMA') || nome.startsWith('SMA') || nome.startsWith('BOLLINGER');

            // Proteção de escala: elimina zeros e nulos para overlays de preço
            const pontosValidos = pontos
                .filter(p => p.value != null && (!isOverlay || Number(p.value) > 0))
                .sort((a, b) => a.time - b.time);

            if (isOverlay) {
                if (pontosValidos.length === 0) return;

                const seriesOptions: any = {
                    color: LINE_COLORS[lineIndexOverlay % LINE_COLORS.length],
                    lineWidth: 2,
                    title: nome,
                    visible: !hiddenIndicatorsRef.current.has(nome),
                    priceScaleId: 'right'
                };
                const lineSeries = mainChart.addSeries(LineSeries, seriesOptions);
                lineSeries.setData(pontosValidos.map(p => ({
                    time: p.time as Time,
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
                        attributionLogo: false
                    },
                    grid: {
                        vertLines: { color: theme.palette.divider },
                        horzLines: { color: theme.palette.divider },
                    },
                    timeScale: {
                        timeVisible: true,
                        secondsVisible: false,
                        rightOffset: 12,
                        barSpacing: 9,
                        minBarSpacing: 3,
                        fixLeftEdge: false,
                        fixRightEdge: false,
                        borderColor: theme.palette.divider,
                    },
                    crosshair: {
                        mode: CrosshairMode.Normal,
                    },
                    leftPriceScale: {
                        visible: false,
                    },
                    rightPriceScale: {
                        visible: true,
                        borderColor: theme.palette.divider,
                        minimumWidth: 70,
                    }
                });
                charts.push({ api: indChart, container, nome });

                const lineSeries = indChart.addSeries(LineSeries, {
                    color: LINE_COLORS[lineIndexOscillator % LINE_COLORS.length],
                    lineWidth: 2,
                    title: nome,
                });

                if (pontosValidos.length > 0) {
                    lineSeries.setData(pontosValidos.map(p => ({
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

        // Sincronização Lógica de Zoom e Pan entre todos os painéis
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

        // Sincronização de Crosshair e Legenda TradingView
        charts.forEach(c => {
            c.api.subscribeCrosshairMove(param => {
                if (param.point === undefined || !param.time || param.point.x < 0 || param.point.y < 0) {
                    charts.forEach(other => {
                        if (other.api !== c.api) {
                            try { (other.api as any).clearCrosshairPosition(); } catch (_) {}
                        }
                    });
                    setTooltipData(null);
                    setHoveredCandle(null);
                    return;
                }

                const timeS = param.time as number;

                // Sincroniza mira vertical nos demais gráficos
                charts.forEach(other => {
                    if (other.api !== c.api) {
                        const targetSeries = other.nome === 'MAIN'
                            ? candleSeriesRef.current
                            : lineSeriesRef.current.get(other.nome);
                        if (targetSeries) {
                            try {
                                (other.api as any).setCrosshairPosition(0, timeS as Time, targetSeries);
                            } catch (_) {}
                        }
                    }
                });

                // Atualiza dados da barra de status da legenda
                const candle = data.candles.find(cd => cd.time === timeS);
                if (candle) {
                    const inds: { [key: string]: number } = {};
                    Object.entries(validIndicators).forEach(([k, pts]) => {
                        if (!hiddenIndicatorsRef.current.has(k)) {
                            const pt = pts.find(p => p.time === timeS);
                            if (pt && Number(pt.value) > 0) {
                                inds[k] = pt.value;
                            }
                        }
                    });
                    setHoveredCandle({
                        time: timeS,
                        open: candle.open,
                        high: candle.high,
                        low: candle.low,
                        close: candle.close,
                        volume: candle.volume,
                        indicators: inds
                    });
                }

                // Tooltip flutuante em eventos de compra/venda
                const isHoveringMarker = param.hoveredObjectId !== undefined;
                const eventoHover = isHoveringMarker ? data.eventos.find(e => e.time === timeS) : undefined;

                if (eventoHover) {
                    const overlaysTexto: string[] = [];
                    const oscillatorsTexto: string[] = [];

                    Object.entries(validIndicators)
                        .filter(([key]) => !hiddenIndicatorsRef.current.has(key))
                        .forEach(([key, pontos]) => {
                            const ponto = pontos.find(p => p.time === timeS);
                            if (ponto && Number(ponto.value) > 0) {
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
    }, [data, theme, chartContainer, validIndicators, updateTimeScaleVisibility]);

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

    // Determina os dados a serem exibidos na barra superior (Hover ou Último Candle)
    const activeInfo = hoveredCandle || latestCandleInfo;
    const isBullish = activeInfo ? activeInfo.close >= activeInfo.open : true;
    const priceColor = isBullish ? '#26a69a' : '#ef5350';
    const deltaPercent = activeInfo && activeInfo.open > 0
        ? (((activeInfo.close - activeInfo.open) / activeInfo.open) * 100).toFixed(2)
        : '0.00';

    return (
        <Box sx={{ flexGrow: 1, position: 'relative', m: 1.5, borderRadius: 2, border: `1px solid ${theme.palette.divider}`, display: 'flex', flexDirection: 'column' }}>

            {/* Barra de Status e Legenda Estilo TradingView */}
            <Box sx={{
                display: 'flex',
                alignItems: 'center',
                flexWrap: 'wrap',
                gap: 1.5,
                px: 2,
                py: 0.8,
                bgcolor: isLightMode ? 'rgba(245, 247, 250, 0.95)' : 'rgba(20, 24, 33, 0.95)',
                borderBottom: `1px solid ${theme.palette.divider}`,
                zIndex: 4,
                fontSize: '0.75rem',
                fontFamily: 'monospace'
            }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.8 }}>
                    <Typography variant="caption" sx={{ fontWeight: 'bold', color: 'primary.main', fontSize: '0.8rem' }}>
                        {data.par} ({data.intervalo})
                    </Typography>
                    {activeInfo && (
                        <Typography variant="caption" sx={{ color: 'text.secondary', fontSize: '0.75rem' }}>
                            • {new Date(activeInfo.time * 1000).toLocaleString('pt-BR')}
                        </Typography>
                    )}
                </Box>

                {activeInfo && (
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.2, flexWrap: 'wrap' }}>
                        <Typography variant="caption">
                            <span style={{ color: theme.palette.text.secondary }}>A:</span> <strong>{activeInfo.open.toFixed(2)}</strong>
                        </Typography>
                        <Typography variant="caption">
                            <span style={{ color: theme.palette.text.secondary }}>M:</span> <strong>{activeInfo.high.toFixed(2)}</strong>
                        </Typography>
                        <Typography variant="caption">
                            <span style={{ color: theme.palette.text.secondary }}>B:</span> <strong>{activeInfo.low.toFixed(2)}</strong>
                        </Typography>
                        <Typography variant="caption" sx={{ color: priceColor }}>
                            <span style={{ color: theme.palette.text.secondary }}>F:</span> <strong>{activeInfo.close.toFixed(2)}</strong> ({isBullish ? '+' : ''}{deltaPercent}%)
                        </Typography>
                        <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                            Vol: <strong>{activeInfo.volume.toFixed(2)}</strong>
                        </Typography>
                    </Box>
                )}

                {/* Valores em tempo real dos indicadores ativos naquele candle */}
                {activeInfo?.indicators && Object.keys(activeInfo.indicators).length > 0 && (
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, ml: 'auto', flexWrap: 'wrap' }}>
                        {Object.entries(activeInfo.indicators).map(([nome, valor], idx) => {
                            const isOverlay = nome.startsWith('EMA') || nome.startsWith('SMA') || nome.startsWith('BOLLINGER');
                            const color = LINE_COLORS[idx % LINE_COLORS.length];
                            return (
                                <Box
                                    key={nome}
                                    sx={{
                                        display: 'inline-flex',
                                        alignItems: 'center',
                                        gap: 0.5,
                                        px: 0.8,
                                        py: 0.2,
                                        borderRadius: 0.8,
                                        bgcolor: isLightMode ? 'rgba(0,0,0,0.05)' : 'rgba(255,255,255,0.08)',
                                        border: `1px solid ${color}40`,
                                        fontSize: '0.7rem'
                                    }}
                                >
                                    <span style={{ width: 6, height: 6, borderRadius: '50%', backgroundColor: color }} />
                                    <span style={{ color: isOverlay ? 'inherit' : color, fontWeight: 'bold' }}>{nome}:</span>
                                    <span>{valor.toFixed(2)}</span>
                                </Box>
                            );
                        })}
                    </Box>
                )}
            </Box>

            {/* Menu Flutuante de Indicadores Ativos */}
            <Box
                onPointerDown={(e) => e.stopPropagation()}
                onClick={(e) => e.stopPropagation()}
                sx={{ position: 'absolute', top: 44, left: 10, zIndex: 6, display: 'flex', flexDirection: 'column', gap: 0.5 }}
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
                    const allNames = Object.keys(validIndicators);
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

            {/* Menu Flutuante de Histórico de Eventos */}
            <Box
                onPointerDown={(e) => e.stopPropagation()}
                onClick={(e) => e.stopPropagation()}
                sx={{ position: 'absolute', top: 44, left: 170, zIndex: 6, display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 1 }}
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

            {/* Containers dos Gráficos: Gráfico Principal de Candles + Osciladores */}
            <div style={{ display: 'flex', flexDirection: 'column', width: '100%', flexGrow: 1, minHeight: 0, overflowY: 'hidden' }}>
                <div ref={setChartContainer} style={{ flexGrow: 1, minHeight: 280 }} />
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

            {/* Tooltip de Marcadores de Compra/Venda */}
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