import React, { useState, useEffect, useCallback } from 'react';
import { Box, TextField, Tabs, Tab, Paper, MenuItem, Typography, CircularProgress, LinearProgress } from '@mui/material';
import Grid from '@mui/material/GridLegacy';
import SearchIcon from '@mui/icons-material/Search';
import type { SymbolInfo } from '../../../types/mexc';
import type { TemplateProps } from '../../../components/common/forms/DynamicForm';
import { getStablecoinPairs } from "../../../services/mexcService.ts";

const PAGE_SIZE = 50;

const SelecaoParTemplate: React.FC<TemplateProps> = ({ formData, handleChange }) => {
    const [pairs, setPairs] = useState<SymbolInfo[]>([]);
    const [loading, setLoading] = useState(true);
    const [loadingMore, setLoadingMore] = useState(false);
    const [stablecoinTab, setStablecoinTab] = useState('USDT');
    const [searchTerm, setSearchTerm] = useState('');
    const [debouncedSearchTerm, setDebouncedSearchTerm] = useState('');
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);

    useEffect(() => {
        const handler = setTimeout(() => {
            setDebouncedSearchTerm(searchTerm);
        }, 1000);

        return () => {
            clearTimeout(handler);
        };
    }, [searchTerm]);

    useEffect(() => {
        setLoading(true);
        setPairs([]);
        setPage(0);
        setHasMore(true);

        getStablecoinPairs(stablecoinTab, 0, PAGE_SIZE, debouncedSearchTerm)
            .then(response => {
                setPairs(response.content);
                setHasMore(!response.last);
            })
            .catch(err => {
                console.error("Falha ao buscar pares de moedas:", err);
            })
            .finally(() => {
                setLoading(false);
            });
    }, [stablecoinTab, debouncedSearchTerm]);

    const fetchMorePairs = useCallback(() => {
        if (loading || loadingMore || !hasMore) return;

        const nextPage = page + 1;
        setPage(nextPage);
        setLoadingMore(true);

        getStablecoinPairs(stablecoinTab, nextPage, PAGE_SIZE, debouncedSearchTerm)
            .then(response => {
                setPairs(prev => [...prev, ...response.content]);
                setHasMore(!response.last);
            })
            .catch(err => {
                console.error("Falha ao buscar mais pares:", err);
            })
            .finally(() => {
                setLoadingMore(false);
            });
    }, [page, hasMore, loading, loadingMore, stablecoinTab, debouncedSearchTerm]);

    const handleScroll = (event: React.UIEvent<HTMLUListElement>) => {
        const target = event.currentTarget;
        if (target.scrollHeight - target.scrollTop <= target.clientHeight + 10) {
            fetchMorePairs();
        }
    };

    const handleTabChange = (_e: React.SyntheticEvent, newValue: string) => {
        setStablecoinTab(newValue);
        setSearchTerm('');
    };

    return (
        <Box>
            <Typography variant="h6" gutterBottom>Selecione o Par de Moedas</Typography>
            <Box sx={{ borderBottom: 1, borderColor: 'divider', width: '100%', mb: 2 }}>
                <Tabs value={stablecoinTab} onChange={handleTabChange} aria-label="stablecoin tabs">
                    <Tab label="USDT" value="USDT" />
                    <Tab label="USDC" value="USDC" />
                    <Tab label="EUR" value="EUR" />
                </Tabs>
            </Box>

            <Grid container spacing={2} alignItems="center" sx={{ mb: 2 }}>
                <Grid item xs={8}>
                    <TextField
                        label="Buscar par..."
                        variant="outlined"
                        size="small"
                        fullWidth
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        InputProps={{
                            startAdornment: <SearchIcon fontSize="small" sx={{ mr: 1 }} />,
                        }}
                    />
                </Grid>
                <Grid item xs={4}>
                    <TextField
                        label="Par Selecionado"
                        variant="filled"
                        size="small"
                        fullWidth
                        value={formData.par || 'Nenhum'}
                        InputProps={{
                            readOnly: true,
                        }}
                    />
                </Grid>
            </Grid>
            {loading ? <CircularProgress sx={{ display: 'block', margin: 'auto', mt: 4 }} /> : (
                <Paper
                    component="ul"
                    variant="outlined"
                    onScroll={handleScroll}
                    sx={{ height: 320, overflow: 'auto', p: 0, m: 0 }}
                >
                    {pairs.map((p, index) => (
                        <MenuItem
                            component="li"
                            key={`${p.symbol}-${index}`}
                            selected={p.symbol === formData.par}
                            onClick={() => handleChange('par', p.symbol)}
                        >
                            {p.symbol}
                        </MenuItem>
                    ))}
                    {loadingMore && <LinearProgress sx={{ position: 'sticky', bottom: 0 }} />}
                </Paper>
            )}
        </Box>
    );
};

export default SelecaoParTemplate;