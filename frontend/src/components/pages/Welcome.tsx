import React from 'react';
import { Paper, Typography } from '@mui/material';
import DynamicDataGrid from '../grid/DynamicDataGrid';
import type {GridColDef, GridRowsProp} from '@mui/x-data-grid';

const myRows: GridRowsProp = [
    { id: 1, product: 'Notebook', price: 4500, category: 'Eletrônicos', stock: 15 },
    { category: 'Acessórios', stock: 30, id: 2, product: 'Teclado Mecânico', price: 350 },
    { stock: 10, id: 3, price: 2800, product: 'Monitor 4K', category: 'Monitores' },
    { price: 180, id: 4, category: 'Acessórios', stock: 50, product: 'Mouse Gamer' },
];

const myColumns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 90 },
    { field: 'product', headerName: 'Produto', width: 200, editable: true },
    { field: 'category', headerName: 'Categoria', width: 150, editable: true },
    {
        field: 'price',
        headerName: 'Preço (R$)',
        type: 'number',
        width: 130,
        editable: true,
        valueFormatter: (value: number) =>
            value ? value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }) : '',
    },
    {
        field: 'stock',
        headerName: 'Estoque',
        type: 'number',
        width: 110,
        editable: true,
    },
];

const Welcome: React.FC = () => {
    return (
        <Paper elevation={2} sx={{ padding: 2, height: 'calc(100vh - 120px)' }}>
            <Typography variant="h5" component="h2" gutterBottom>
                Painel de Controle
            </Typography>
            <DynamicDataGrid initialRows={myRows} gridColumns={myColumns} />
        </Paper>
    );
};

export default Welcome;