import { useEffect, useState, useCallback, useRef, type ComponentType } from 'react';
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
    gridColumns?: GridColDef<T>[];
    columnsFactory?: (refresh: () => void) => GridColDef<T>[];
    FormComponent: ComponentType<{ entityId: string | null; onClose: () => void; onSave: () => void; }>;
    // Novas propriedades de Arquitetura para Polling Otimizado
    pollingInterval?: number;
    pollingCondition?: (entities: T[]) => boolean;
}

export default function GenericCrudPage<T extends BaseEntity>({
                                                                  title,
                                                                  description,
                                                                  fetcher,
                                                                  deleter,
                                                                  gridColumns,
                                                                  columnsFactory,
                                                                  FormComponent,
                                                                  pollingInterval,
                                                                  pollingCondition
                                                              }: GenericCrudPageProps<T>) {
    const [entities, setEntities] = useState<T[]>([]);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [selectedEntityId, setSelectedEntityId] = useState<string | null>(null);

    // Ref para blindar o polling contra Stale Closures e evitar re-renders do setInterval
    const entitiesCache = useRef<T[]>([]);

    const loadEntities = useCallback(() => {
        fetcher()
            .then(data => {
                setEntities(data);
                entitiesCache.current = data; // Atualiza a memória estática para o loop
            })
            .catch(error => console.error(`Falha ao buscar dados para ${title}:`, error));
    }, [fetcher, title]);

    // Initial Fetch
    useEffect(() => {
        loadEntities();
    }, [loadEntities]);

    // Smart Polling Engine
    useEffect(() => {
        if (!pollingInterval) return;

        const intervalId = setInterval(() => {
            const currentCache = entitiesCache.current;

            // Short-circuit: aborta a requisição HTTP se as condições de estado não exigirem
            if (pollingCondition && !pollingCondition(currentCache)) {
                return;
            }

            loadEntities();
        }, pollingInterval);

        return () => clearInterval(intervalId);
    }, [pollingInterval, pollingCondition, loadEntities]);

    const columns = columnsFactory ? columnsFactory(loadEntities) : (gridColumns || []);

    const handleEdit = useCallback((id: GridRowId) => {
        setSelectedEntityId(String(id));
        setIsModalOpen(true);
    }, []);

    const handleCreate = useCallback(() => {
        setSelectedEntityId(null);
        setIsModalOpen(true);
    }, []);

    const handleCloseModal = useCallback(() => {
        setIsModalOpen(false);
        setSelectedEntityId(null);
    }, []);

    const handleSave = useCallback(() => {
        handleCloseModal();
        loadEntities();
    }, [handleCloseModal, loadEntities]);

    const handleDelete = useCallback(async (id: GridRowId) => {
        if (deleter) {
            try {
                await deleter(String(id));
                loadEntities();
            } catch (error) {
                console.error(`Falha ao excluir o item ${id}:`, error);
            }
        }
    }, [deleter, loadEntities]);

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