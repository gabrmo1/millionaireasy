import React, { useMemo, useState, useEffect } from 'react';
import { Box, TextField, Tabs, Tab, Paper, MenuItem, Typography, CircularProgress } from '@mui/material';
import Grid from '@mui/material/GridLegacy';
import SearchIcon from '@mui/icons-material/Search';
import type { SymbolInfo } from '../../../types/mexc';
import type { TemplateProps } from '../../../components/common/forms/DynamicForm';
import { getStablecoinPairs } from "../../../services/mexcService.ts";

const SelecaoParTemplate: React.FC<TemplateProps> = ({ formData, handleChange }) => {
    const [allPairs, setAllPairs] = useState<SymbolInfo[]>([]);
    const [loading, setLoading] = useState(false);
    const [stablecoinTab, setStablecoinTab] = useState('USDT');
    const [searchTerm, setSearchTerm] = useState('');

    useEffect(() => {
        setLoading(true);
        getStablecoinPairs()
            .then(setAllPairs)
            .catch(err => console.error("Falha ao buscar pares de moedas:", err))
            .finally(() => setLoading(false));
    }, []);

    const filteredPairs = useMemo(() => {
        return allPairs
            .filter(p => p.quoteAsset === stablecoinTab)
            .filter(p => p.symbol.toLowerCase().includes(searchTerm.toLowerCase()));
    }, [allPairs, stablecoinTab, searchTerm]);

    return (
        <Box>
            <Typography variant="h6" gutterBottom>Selecione o Par de Moedas</Typography>
            <Box sx={{ borderBottom: 1, borderColor: 'divider', width: '100%', mb: 2 }}>
                <Tabs value={stablecoinTab} onChange={(_e, newValue) => setStablecoinTab(newValue)} aria-label="stablecoin tabs">
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
                <Paper variant="outlined" sx={{ height: 320, overflow: 'auto' }}>
                    {filteredPairs.map(p => (
                        <MenuItem
                            key={p.symbol}
                            selected={p.symbol === formData.par}
                            onClick={() => handleChange('par', p.symbol)}
                        >
                            {p.symbol}
                        </MenuItem>
                    ))}
                </Paper>
            )}
        </Box>
    );
};

export default SelecaoParTemplate;