import React, { useEffect, useRef, useState } from 'react';
import {
    Dialog,
    DialogTitle,
    DialogContent,
    IconButton,
    Box,
    CircularProgress,
    Typography,
    useTheme
} from '@mui/material';
import { Close as CloseIcon } from '@mui/icons-material';
import {
    createChart,
    ColorType,
    type IChartApi,
    type CandlestickData,
    type SeriesMarker,
    type LineData,
    type Time,
    CandlestickSeries,
    LineSeries,
    createSeriesMarkers // Plugin nativo da v5
} from 'lightweight-charts';
import { getMonitoramentoData } from '../../services/operacaoService';
import type { MonitoramentoDataDTO, EventoChartDTO } from '../../types/monitoramento';

interface MonitoramentoModalProps {
    open: boolean;
    onClose: () => void;
    operacaoId: string | null;
}

const LINE_COLORS = ['#2962FF', '#E91E63', '#FF6D00', '#00E676', '#AA00FF'];

const MonitoramentoModal: React.FC<MonitoramentoModalProps> = ({ open, onClose, operacaoId }) => {
    const theme = useTheme();
    const chartContainerRef = useRef<HTMLDivElement>(null);
    const chartRef = useRef<IChartApi | null>(null);

    const [loading, setLoading] = useState(false);
    const [data, setData] = useState<MonitoramentoDataDTO | null>(null);
    const [tooltipData, setTooltipData] = useState<{
        title: string;
        body: string[];
        x: number;
        y: number;
        visible: boolean;
    } | null>(null);

    // 1. Fetch Data
    useEffect(() => {
        if (open && operacaoId) {
            setLoading(true);
            getMonitoramentoData(operacaoId)
                .then(setData)
                .catch(err => console.error("Erro ao carregar monitoramento:", err))
                .finally(() => setLoading(false));
        } else {
            setData(null);
        }
    }, [open, operacaoId]);

    // 2. Render Chart
    useEffect(() => {
        if (!data || !chartContainerRef.current) return;

        // Cleanup
        if (chartRef.current) {
            chartRef.current.remove();
            chartRef.current = null;
        }

        const chart = createChart(chartContainerRef.current, {
            layout: {
                background: { type: ColorType.Solid, color: theme.palette.background.paper },
                textColor: theme.palette.text.primary,
            },
            grid: {
                vertLines: { color: theme.palette.divider },
                horzLines: { color: theme.palette.divider },
            },
            width: chartContainerRef.current.clientWidth,
            height: 500,
            timeScale: {
                timeVisible: true,
                secondsVisible: false,
            },
        });

        chartRef.current = chart;

        // --- A. Série de Candles ---
        const candleSeries = chart.addSeries(CandlestickSeries, {
            upColor: '#26a69a',
            downColor: '#ef5350',
            borderVisible: false,
            wickUpColor: '#26a69a',
            wickDownColor: '#ef5350'
        });

        const candleData: CandlestickData[] = data.candles.map(c => ({
            time: c.time as Time,
            open: c.open,
            high: c.high,
            low: c.low,
            close: c.close
        }));

        candleSeries.setData(candleData);

        // --- B. Markers com "Time Snapping" ---
        // Aqui está o segredo: Ajustamos o tempo do evento para o tempo do candle
        const markers: SeriesMarker<Time>[] = [];

        data.eventos.forEach(evt => {
            // Encontra o candle que começou ANTES ou NO MOMENTO do evento
            // Assumindo que data.candles está ordenado por tempo
            // Iteramos de trás para frente para achar o mais recente compatível
            let snappedTime: number | null = null;

            for (let i = data.candles.length - 1; i >= 0; i--) {
                if (data.candles[i].time <= evt.time) {
                    snappedTime = data.candles[i].time;
                    break;
                }
            }

            if (snappedTime) {
                markers.push({
                    time: snappedTime as Time,
                    position: evt.tipo === 'COMPRA' ? 'belowBar' : 'aboveBar',
                    color: evt.cor,
                    shape: evt.tipo === 'COMPRA' ? 'arrowUp' : 'arrowDown',
                    text: evt.tipo === 'COMPRA' ? 'C' : 'V',
                    size: 2,
                    // Guardamos o ID original para tooltip se necessário (hack opcional)
                    // id: evt.time
                });
            }
        });

        // Adiciona os markers usando a função da v5
        createSeriesMarkers(candleSeries, markers);

        // --- C. Indicadores (Linhas) ---
        let colorIndex = 0;
        Object.entries(data.indicadores).forEach(([nome, pontos]) => {
            if (nome.includes('EMA') || nome.includes('SMA')) {
                const lineSeries = chart.addSeries(LineSeries, {
                    color: LINE_COLORS[colorIndex % LINE_COLORS.length],
                    lineWidth: 1,
                    title: nome,
                });

                const lineData: LineData[] = pontos.map(p => ({
                    time: p.time as Time,
                    value: p.value
                }));

                lineSeries.setData(lineData);
                colorIndex++;
            }
        });

        // --- D. Tooltip Inteligente ---
        const indicadoresMap = new Map<number, Record<string, number>>();
        Object.entries(data.indicadores).forEach(([nome, pontos]) => {
            pontos.forEach(p => {
                const existing = indicadoresMap.get(p.time) || {};
                existing[nome] = p.value;
                indicadoresMap.set(p.time, existing);
            });
        });

        // Mapa de eventos: Usamos o tempo "snapped" para indexar também
        const eventsMap = new Map<number, EventoChartDTO[]>();
        data.eventos.forEach(evt => {
            // Replicamos a lógica de snap para o tooltip funcionar
            let snappedTime: number | null = null;
            for (let i = data.candles.length - 1; i >= 0; i--) {
                if (data.candles[i].time <= evt.time) {
                    snappedTime = data.candles[i].time;
                    break;
                }
            }
            if (snappedTime) {
                const list = eventsMap.get(snappedTime) || [];
                list.push(evt);
                eventsMap.set(snappedTime, list);
            }
        });

        const candlesMap = new Map<number, any>();
        data.candles.forEach(c => candlesMap.set(c.time, c));

        chart.subscribeCrosshairMove(param => {
            if (
                param.point === undefined ||
                !param.time ||
                param.point.x < 0 ||
                param.point.x > chartContainerRef.current!.clientWidth ||
                param.point.y < 0 ||
                param.point.y > chartContainerRef.current!.clientHeight
            ) {
                setTooltipData(prev => prev ? { ...prev, visible: false } : null);
                return;
            }

            const time = param.time as number;
            const candle = candlesMap.get(time);
            const indics = indicadoresMap.get(time);
            const eventos = eventsMap.get(time); // Pode haver mais de um evento no mesmo candle

            if (!candle) return;

            const bodyLines = [
                `O: ${candle.open} | H: ${candle.high} | L: ${candle.low} | C: ${candle.close}`,
            ];

            if (indics) {
                Object.entries(indics).forEach(([k, v]) => {
                    bodyLines.push(`${k}: ${v.toFixed(2)}`);
                });
            }

            if (eventos) {
                bodyLines.push('---');
                eventos.forEach(evt => {
                    bodyLines.push(`${evt.tipo}: ${evt.tooltip}`);
                });
            }

            setTooltipData({
                title: new Date(time * 1000).toLocaleString(),
                body: bodyLines,
                x: param.point.x,
                y: param.point.y,
                visible: true
            });
        });

        chart.timeScale().fitContent();

        const handleResize = () => {
            if (chartContainerRef.current) {
                chart.applyOptions({ width: chartContainerRef.current.clientWidth });
            }
        };
        window.addEventListener('resize', handleResize);

        return () => {
            window.removeEventListener('resize', handleResize);
            if (chartRef.current) {
                chartRef.current.remove();
                chartRef.current = null;
            }
        };
    }, [data, theme]);

    return (
        <Dialog
            open={open}
            onClose={onClose}
            maxWidth="lg"
            fullWidth
            PaperProps={{ sx: { height: '80vh', display: 'flex', flexDirection: 'column' } }}
        >
            <DialogTitle component="div" sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="h6">
                    Monitoramento: {data ? `${data.par} - ${data.intervalo}` : 'Carregando...'}
                </Typography>
                <IconButton onClick={onClose}><CloseIcon /></IconButton>
            </DialogTitle>

            <DialogContent dividers sx={{ p: 0, position: 'relative', flex: 1, overflow: 'hidden' }}>
                {loading && (
                    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
                        <CircularProgress />
                    </Box>
                )}
                <div ref={chartContainerRef} style={{ width: '100%', height: '100%' }} />

                {tooltipData && tooltipData.visible && (
                    <div
                        style={{
                            position: 'absolute',
                            left: tooltipData.x + 10,
                            top: tooltipData.y + 10,
                            zIndex: 1000,
                            background: 'rgba(255, 255, 255, 0.95)',
                            border: '1px solid #ccc',
                            padding: '10px',
                            borderRadius: '4px',
                            pointerEvents: 'none',
                            boxShadow: '0 4px 6px rgba(0,0,0,0.1)',
                            fontSize: '12px',
                            color: '#000',
                            fontFamily: 'monospace',
                            minWidth: '200px'
                        }}
                    >
                        <div style={{ fontWeight: 'bold', marginBottom: '8px', borderBottom: '1px solid #eee', paddingBottom: '4px' }}>
                            {tooltipData.title}
                        </div>
                        {tooltipData.body.map((line, i) => (
                            <div key={i} style={{
                                marginBottom: '2px',
                                color: line.startsWith('COMPRA') ? '#2e7d32' : line.startsWith('VENDA') ? '#c62828' : '#333',
                                fontWeight: line.includes('---') ? 'bold' : 'normal'
                            }}>
                                {line}
                            </div>
                        ))}
                    </div>
                )}
            </DialogContent>
        </Dialog>
    );
};

export default MonitoramentoModal;