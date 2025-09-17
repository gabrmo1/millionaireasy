import * as React from 'react';
import {
    DataGrid,
    GridToolbar,
    type GridColDef,
    type GridRowModesModel,
    GridRowModes,
    type GridRowId,
    GridRowEditStopReasons,
    type GridEventListener,
    GridActionsCellItem,
    type GridValidRowModel,
} from '@mui/x-data-grid';
import { Box } from '@mui/material';
import SaveIcon from '@mui/icons-material/Save';
import CancelIcon from '@mui/icons-material/Close';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/DeleteOutlined';
import ConfirmationModal from '../common/modals/ConfirmationModal';
import type { BaseEntity } from '../../types/common';

interface DynamicDataGridProps<T extends GridValidRowModel> {
    initialRows: T[];
    gridColumns: GridColDef<T>[];
    onEdit?: (id: GridRowId) => void; // <-- Nova propriedade
    onDelete?: (id: GridRowId) => Promise<void>;
}

export default function DynamicDataGrid<T extends BaseEntity>(props: DynamicDataGridProps<T>) {
    const [rows, setRows] = React.useState<T[]>(props.initialRows);
    const [rowModesModel, setRowModesModel] = React.useState<GridRowModesModel>({});
    const [isDeleteModalOpen, setDeleteModalOpen] = React.useState(false);
    const [selectedRow, setSelectedRow] = React.useState<T | null>(null);

    React.useEffect(() => { setRows(props.initialRows); }, [props.initialRows]);

    const handleRowEditStop: GridEventListener<'rowEditStop'> = (params, event) => {
        if (params.reason === GridRowEditStopReasons.rowFocusOut) {
            event.defaultMuiPrevented = true;
        }
    };

    const handleEditClick = (id: GridRowId) => () => {
        if (props.onEdit) {
            props.onEdit(id);
        } else {
            setRowModesModel({ ...rowModesModel, [id]: { mode: GridRowModes.Edit } });
        }
    };

    const handleSaveClick = (id: GridRowId) => () => {
        setRowModesModel({ ...rowModesModel, [id]: { mode: GridRowModes.View } });
    };

    const handleDeleteClick = (row: T) => () => {
        setSelectedRow(row);
        setDeleteModalOpen(true);
    };

    const handleConfirmDelete = async () => {
        if (selectedRow && props.onDelete) {
            await props.onDelete(selectedRow.id);
        }
        setSelectedRow(null);
    };

    const handleCancelClick = (id: GridRowId) => () => {
        setRowModesModel({
            ...rowModesModel,
            [id]: { mode: GridRowModes.View, ignoreModifications: true },
        });

        const editedRow = rows.find((row) => row.id === id);
        if (editedRow && (editedRow as any).isNew) {
            setRows(rows.filter((row) => row.id !== id));
        }
    };

    const processRowUpdate = (newRow: T): T => {
        setRows(rows.map((row) => (row.id === newRow.id ? newRow : row)));
        return newRow;
    };

    const handleRowModesModelChange = (newRowModesModel: GridRowModesModel) => {
        setRowModesModel(newRowModesModel);
    };

    const columnsWithActions: GridColDef<T>[] = [
        ...props.gridColumns,
        {
            field: 'actions',
            type: 'actions',
            headerName: 'Ações',
            width: 100,
            cellClassName: 'actions',
            getActions: ({ id, row }) => {
                const isInEditMode = rowModesModel[id]?.mode === GridRowModes.Edit;

                if (isInEditMode) {
                    return [
                        <GridActionsCellItem
                            icon={<SaveIcon />}
                            label="Salvar"
                            onClick={handleSaveClick(id)}
                            color="primary"
                            key={`save-${id}`}
                        />,
                        <GridActionsCellItem
                            icon={<CancelIcon />}
                            label="Cancelar"
                            className="textPrimary"
                            onClick={handleCancelClick(id)}
                            color="inherit"
                            key={`cancel-${id}`}
                        />,
                    ];
                }

                return [
                    <GridActionsCellItem
                        icon={<EditIcon />}
                        label="Editar"
                        className="textPrimary"
                        onClick={handleEditClick(id)}
                        color="inherit"
                        key={`edit-${id}`}
                    />,
                    <GridActionsCellItem
                        icon={<DeleteIcon />}
                        label="Excluir"
                        onClick={handleDeleteClick(row)}
                        color="inherit"
                        key={`delete-${id}`}
                    />,
                ];
            },
        },
    ];

    return (
        <Box
            sx={{
                height: '100%',
                width: '100%',
                '& .actions': { color: 'text.secondary' },
                '& .textPrimary': { color: 'text.primary' },
            }}
        >
            <DataGrid<T>
                rows={rows}
                columns={columnsWithActions}
                density="compact"
                editMode="row"
                rowModesModel={rowModesModel}
                onRowModesModelChange={handleRowModesModelChange}
                onRowEditStop={handleRowEditStop}
                processRowUpdate={processRowUpdate}
                initialState={{
                    pagination: { paginationModel: { pageSize: 20 } },
                }}
                pageSizeOptions={[20, 50, 100]}
                disableRowSelectionOnClick
                showToolbar // MANTIDO
                slots={{
                    toolbar: GridToolbar,
                }}
                slotProps={{
                    toolbar: {
                        sx: {
                            backgroundColor: 'lightslategray',
                            color: (theme) => theme.palette.primary.contrastText,
                            '& .MuiButton-root, & .MuiSvgIcon-root': { // Garante que botões e ícones fiquem brancos
                                color: (theme) => theme.palette.primary.contrastText,
                            },
                        },
                    },
                }}
            />
            {selectedRow && (
                <ConfirmationModal
                    open={isDeleteModalOpen}
                    title="Confirmar Exclusão"
                    message={`Você realmente deseja excluir ${selectedRow.nome || selectedRow.par || 'este item'}?`}
                    onConfirm={handleConfirmDelete}
                    onClose={() => setDeleteModalOpen(false)}
                />
            )}
        </Box>
    );
}