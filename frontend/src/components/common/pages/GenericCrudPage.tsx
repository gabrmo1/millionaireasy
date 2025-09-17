import { useEffect, useState, useCallback } from 'react';
import { Paper, Typography, Box, Alert, Fab } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import type {GridColDef, GridRowId} from '@mui/x-data-grid';
import { useNavigate } from 'react-router-dom';
import DynamicDataGrid from '../../grid/DynamicDataGrid';
import type {BaseEntity} from '../../../types/common';

interface GenericCrudPageProps<T extends BaseEntity> {
    title: string;
    description?: string;
    fetcher: () => Promise<T[]>;
    deleter?: (id: string) => Promise<void>;
    gridColumns: GridColDef[];
    createRoute: string;
    editRoute: string; // <-- Nova propriedade
}

export default function GenericCrudPage<T extends BaseEntity>({
                                                                  title,
                                                                  description,
                                                                  fetcher,
                                                                  deleter,
                                                                  gridColumns,
                                                                  createRoute,
                                                                  editRoute, // <-- Nova propriedade
                                                              }: GenericCrudPageProps<T>) {
    const [entities, setEntities] = useState<T[]>([]);
    const navigate = useNavigate();

    const loadEntities = useCallback(() => {
        fetcher()
            .then(setEntities)
            .catch(error => console.error(`Falha ao buscar dados para ${title}:`, error));
    }, [fetcher, title]);

    useEffect(() => {
        loadEntities();
    }, [loadEntities]);

    const handleEdit = (id: GridRowId) => {
        navigate(`/${editRoute}/${id}`);
    };

    const handleDelete = async (id: GridRowId) => {
        if (deleter) {
            try {
                await deleter(String(id));
                loadEntities();
            } catch (error) {
                console.error(`Falha ao excluir o item ${id}:`, error);
            }
        }
    };

    return (
        <Paper
            elevation={2}
            sx={{
                height: 'calc(100vh - 112px)',
                display: 'flex',
                flexDirection: 'column',
                overflow: 'hidden'
            }}
        >
            {/* Cabeçalho com Título e Botão */}
            <Box sx={{ borderBottom: 1, borderColor: 'divider', bgcolor: (theme) => theme.palette.action.hover, position: 'relative' }}>
                <Box sx={{ p: 2 }}>
                    <Typography variant="h5" component="h2">
                        {`${title} - Visualizar`}
                    </Typography>
                </Box>
                <Fab
                    variant="extended"
                    color="primary"
                    aria-label="add"
                    onClick={() => navigate(createRoute)}
                    sx={{
                        position: 'absolute',
                        top: '50%',
                        right: '32px',
                        transform: 'translateY(-50%)',
                    }}
                >
                    <AddIcon sx={{ mr: 1 }} />
                    Criar {title}
                </Fab>
            </Box>

            {/* Caixa de Descrição */}
            {description && (
                <Box sx={{ p: 1, borderBottom: 1, borderColor: 'divider' }}>
                    <Alert severity="info">
                        {description}
                    </Alert>
                </Box>
            )}

            {/* Corpo com a Grade */}
            <Box sx={{ flexGrow: 1, p: '32px', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
                <Box sx={{ flexGrow: 1, width: '100%' }}>
                    <DynamicDataGrid
                        initialRows={entities}
                        gridColumns={gridColumns}
                        onEdit={handleEdit}
                        onDelete={handleDelete}
                    />
                </Box>
            </Box>
        </Paper>
    );
}