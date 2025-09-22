import React, { useMemo, useState } from 'react';
import { Box, TextField, Tabs, Tab, Paper, MenuItem, FormHelperText, Typography } from '@mui/material';
import Grid from '@mui/material/GridLegacy';
import SearchIcon from '@mui/icons-material/Search';
import type { SymbolInfo } from '../../../types/mexc';

interface Step1Props {
    formData: { par?: string };
    errors: { par?: string | null };
    allPairs: SymbolInfo[];
    onPairChange: (par: string) => void;
}

const Step1_SelecaoPar: React.FC<Step1Props> = ({ formData, errors, allPairs, onPairChange }) => {
    const [stablecoinTab, setStablecoinTab] = useState('USDT');
    const [searchTerm, setSearchTerm] = useState('');

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

            <Paper variant="outlined" sx={{ height: 320, overflow: 'auto' }}>
                {filteredPairs.map(p => (
                    <MenuItem
                        key={p.symbol}
                        selected={p.symbol === formData.par}
                        onClick={() => onPairChange(p.symbol)}
                    >
                        {p.symbol}
                    </MenuItem>
                ))}
            </Paper>
            {errors.par && <FormHelperText error sx={{ mt: 1 }}>{errors.par}</FormHelperText>}
        </Box>
    );
};

export default Step1_SelecaoPar;