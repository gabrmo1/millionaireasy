import { useEffect, useState, useCallback, type ComponentType } from 'react';
import { Paper, Typography, Box, Alert, Fab } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import type { GridColDef, GridRowId } from '@mui/x-data-grid';
import DynamicDataGrid from '../../grid/DynamicDataGrid';
import type { BaseEntity } from '../../../types/common';
import FormModal from '../modals/FormModal';

interface GenericCrudPageProps<T extends BaseEntity> {
    title: string;
    description?: string;
    fetcher: () => Promise<T[]>;
    deleter?: (id: string) => Promise<void>;
    gridColumns?: GridColDef[]; // Agora opcional
    columnsFactory?: (refresh: () => void) => GridColDef[]; // Nova prop
    FormComponent: ComponentType<{ entityId: string | null; onClose: () => void; onSave: () => void; }>;
}

export default function GenericCrudPage<T extends BaseEntity>({
                                                                  title,
                                                                  description,
                                                                  fetcher,
                                                                  deleter,
                                                                  gridColumns,
                                                                  columnsFactory,
                                                                  FormComponent,
                                                              }: GenericCrudPageProps<T>) {
    const [entities, setEntities] = useState<T[]>([]);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [selectedEntityId, setSelectedEntityId] = useState<string | null>(null);

    const loadEntities = useCallback(() => {
        fetcher()
            .then(setEntities)
            .catch(error => console.error(`Falha ao buscar dados para ${title}:`, error));
    }, [fetcher, title]);

    useEffect(() => {
        loadEntities();
    }, [loadEntities]);

    // Define as colunas: usa a factory se existir (passando o refresh), senão usa as colunas estáticas
    const columns = columnsFactory ? columnsFactory(loadEntities) : (gridColumns || []);

    const handleEdit = (id: GridRowId) => {
        setSelectedEntityId(String(id));
        setIsModalOpen(true);
    };

    const handleCreate = () => {
        setSelectedEntityId(null);
        setIsModalOpen(true);
    };

    const handleCloseModal = () => {
        setIsModalOpen(false);
        setSelectedEntityId(null);
    };

    const handleSave = () => {
        handleCloseModal();
        loadEntities(); // Recarrega os dados do grid após salvar
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
        <>
            <Paper
                elevation={2}
                sx={{
                    height: 'calc(100vh - 112px)',
                    display: 'flex',
                    flexDirection: 'column',
                    overflow: 'hidden'
                }}
            >
                <Box sx={{ borderBottom: 1, borderColor: 'divider', bgcolor: (theme) => theme.palette.action.hover, position: 'relative' }}>
                    <Box sx={{ p: 2 }}>
                        <Typography variant="h5" component="h2">{title}</Typography>
                    </Box>
                    <Fab
                        variant="extended"
                        color="primary"
                        aria-label="add"
                        onClick={handleCreate}
                        sx={{ position: 'absolute', top: '50%', right: '32px', transform: 'translateY(-50%)' }}
                    >
                        <AddIcon sx={{ mr: 1 }} />
                        Criar {title}
                    </Fab>
                </Box>

                {description && (
                    <Box sx={{ p: 1, borderBottom: 1, borderColor: 'divider' }}>
                        <Alert severity="info">{description}</Alert>
                    </Box>
                )}

                <Box sx={{ flexGrow: 1, p: '32px', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
                    <Box sx={{ flexGrow: 1, width: '100%' }}>
                        <DynamicDataGrid
                            initialRows={entities}
                            gridColumns={columns}
                            onEdit={handleEdit}
                            onDelete={handleDelete}
                        />
                    </Box>
                </Box>
            </Paper>

            <FormModal
                open={isModalOpen}
                onClose={handleCloseModal}
                title={selectedEntityId ? `Editar ${title}` : `Criar ${title}`}
            >
                <FormComponent
                    entityId={selectedEntityId}
                    onClose={handleCloseModal}
                    onSave={handleSave}
                />
            </FormModal>
        </>
    );
}