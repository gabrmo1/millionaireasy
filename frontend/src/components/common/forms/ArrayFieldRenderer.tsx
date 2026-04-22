import React, { useState } from 'react';
import {
    Box,
    Button,
    Card,
    CardContent,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    IconButton,
    Typography,
    Stack
} from '@mui/material';
import Grid from '@mui/material/Grid';
import { Add, Delete, Edit } from '@mui/icons-material';
import type { FormFieldMetadata } from '../../../types/formMetadata';
import FormFieldRenderer from './FormFieldRenderer';

interface ArrayFieldRendererProps {
    field: FormFieldMetadata;
    value: any[];
    onChange: (newValue: any[]) => void;
    error?: string | null;
}

const ArrayFieldRenderer: React.FC<ArrayFieldRendererProps> = ({ field, value = [], onChange, error }) => {
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [currentItem, setCurrentItem] = useState<Record<string, any>>({});
    const [editingIndex, setEditingIndex] = useState<number | null>(null);
    const [itemErrors, setItemErrors] = useState<Record<string, string | null>>({});

    const arrayConfig = field.arrayConfig;

    if (!arrayConfig) {
        return <Typography color="error">Configuração do array ausente</Typography>;
    }

    const handleOpenModal = (item?: any, index?: number) => {
        if (item && index !== undefined) {
            setCurrentItem({ ...item });
            setEditingIndex(index);
        } else {
            setCurrentItem({});
            setEditingIndex(null);
        }
        setItemErrors({});
        setIsModalOpen(true);
    };

    const handleCloseModal = () => {
        setIsModalOpen(false);
        setCurrentItem({});
        setEditingIndex(null);
    };

    // Gerencia mudanças nos inputs de DENTRO do modal
    const handleItemChange = (fieldName: string, fieldValue: any) => {
        setCurrentItem(prev => ({
            ...prev,
            [fieldName]: fieldValue
        }));
        // Limpa erro ao digitar
        if (itemErrors[fieldName]) {
            setItemErrors(prev => ({ ...prev, [fieldName]: null }));
        }
    };

    const handleSaveItem = () => {
        // Validação simples dos campos do item (Obrigatórios)
        const newErrors: Record<string, string | null> = {};
        let isValid = true;

        arrayConfig.schema.forEach(schemaField => {
            if (!schemaField.nullable && !schemaField.hidden) {
                const val = currentItem[schemaField.field];
                if (val === undefined || val === '' || val === null) {
                    newErrors[schemaField.field] = 'Obrigatório';
                    isValid = false;
                }
            }
        });

        if (!isValid) {
            setItemErrors(newErrors);
            return;
        }

        // Salva no array principal
        const newValue = [...value];
        if (editingIndex !== null) {
            newValue[editingIndex] = currentItem;
        } else {
            newValue.push(currentItem);
        }

        onChange(newValue);
        handleCloseModal();
    };

    const handleRemoveItem = (index: number) => {
        const newValue = value.filter((_, i) => i !== index);
        onChange(newValue);
    };

    // Helper para gerar um "Resumo" visual do item no Card
    const getItemSummary = (item: any) => {
        // Tenta pegar o primeiro campo string do schema para usar como título
        const titleField = arrayConfig.schema.find(f => f.type === 'string' || f.type === 'enum');
        if (titleField && item[titleField.field]) {
            return String(item[titleField.field]);
        }
        return `${arrayConfig.itemLabel} #${value.indexOf(item) + 1}`;
    };

    return (
        <Box sx={{ mt: 1, mb: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                <Typography variant="subtitle2" color="textSecondary">
                    {field.label}
                </Typography>
                <Button
                    startIcon={<Add />}
                    variant="outlined"
                    size="small"
                    onClick={() => handleOpenModal()}
                >
                    Adicionar {arrayConfig.itemLabel}
                </Button>
            </Box>

            {error && (
                <Typography color="error" variant="caption" display="block" sx={{ mb: 1 }}>
                    {error}
                </Typography>
            )}

            <Stack spacing={1}>
                {value.length === 0 && (
                    <Typography variant="body2" color="textSecondary" sx={{ fontStyle: 'italic', p: 1 }}>
                        Nenhum item adicionado.
                    </Typography>
                )}

                {value.map((item, index) => (
                    <Card key={index} variant="outlined">
                        <CardContent sx={{ py: 1, px: 2, '&:last-child': { pb: 1 }, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <Typography variant="body2" fontWeight="medium">
                                {getItemSummary(item)}
                            </Typography>
                            <Box>
                                <IconButton size="small" onClick={() => handleOpenModal(item, index)}>
                                    <Edit fontSize="small" />
                                </IconButton>
                                <IconButton size="small" color="error" onClick={() => handleRemoveItem(index)}>
                                    <Delete fontSize="small" />
                                </IconButton>
                            </Box>
                        </CardContent>
                    </Card>
                ))}
            </Stack>

            {/* MODAL DE EDIÇÃO/CRIAÇÃO DO ITEM */}
            <Dialog open={isModalOpen} onClose={handleCloseModal} maxWidth="sm" fullWidth>
                <DialogTitle>
                    {editingIndex !== null ? 'Editar' : 'Novo'} {arrayConfig.itemLabel}
                </DialogTitle>
                <DialogContent dividers>
                    <Grid container spacing={2} sx={{ mt: 0.5 }}>
                        {arrayConfig.schema.map((schemaField) => (
                            <Grid size={{ xs: 12, sm: Number(schemaField.fieldSize) || 12 }} key={schemaField.field}>
                                <FormFieldRenderer
                                    field={schemaField}
                                    formData={currentItem}
                                    errors={itemErrors}
                                    handleChange={handleItemChange}
                                />
                            </Grid>
                        ))}
                    </Grid>
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleCloseModal}>Cancelar</Button>
                    <Button onClick={handleSaveItem} variant="contained">Confirmar</Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default ArrayFieldRenderer;