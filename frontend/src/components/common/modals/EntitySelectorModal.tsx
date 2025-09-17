import { useEffect, useState } from 'react';
import { Modal, Box, Typography, List, ListItem, ListItemButton, ListItemText, Paper, CircularProgress } from '@mui/material';
import type {BaseEntity} from '../../../types/common';

interface EntitySelectorModalProps<T extends BaseEntity> {
    open: boolean;
    title: string;
    fetcher: () => Promise<T[]>;
    displayAttribute: keyof T;
    onClose: () => void;
    onSelect: (entity: T) => void;
}

const style = {
    position: 'absolute' as 'absolute',
    top: '50%',
    left: '50%',
    transform: 'translate(-50%, -50%)',
    width: 500,
    bgcolor: 'background.paper',
    boxShadow: 24,
    p: 4,
    borderRadius: 2,
};

export default function EntitySelectorModal<T extends BaseEntity>({
                                                                      open,
                                                                      title,
                                                                      fetcher,
                                                                      displayAttribute,
                                                                      onClose,
                                                                      onSelect,
                                                                  }: EntitySelectorModalProps<T>) {
    const [entities, setEntities] = useState<T[]>([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (open) {
            setLoading(true);
            fetcher()
                .then(setEntities)
                .catch(err => console.error(`Falha ao buscar entidades para o modal ${title}:`, err))
                .finally(() => setLoading(false));
        }
    }, [open, fetcher, title]);

    const handleSelectEntity = (entity: T) => {
        onSelect(entity);
        onClose();
    };

    return (
        <Modal open={open} onClose={onClose}>
            <Box sx={style}>
                <Typography variant="h6" component="h2">{title}</Typography>
                <Paper sx={{ maxHeight: 300, overflow: 'auto', mt: 2 }}>
                    {loading ? (
                        <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}>
                            <CircularProgress />
                        </Box>
                    ) : (
                        <List>
                            {entities.map((entity) => (
                                <ListItem key={entity.id} disablePadding>
                                    <ListItemButton onClick={() => handleSelectEntity(entity)}>
                                        <ListItemText
                                            primary={entity[displayAttribute]}
                                            secondary={`ID: ${entity.id}`}
                                        />
                                    </ListItemButton>
                                </ListItem>
                            ))}
                        </List>
                    )}
                </Paper>
            </Box>
        </Modal>
    );
}