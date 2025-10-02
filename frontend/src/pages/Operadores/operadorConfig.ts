import type {GridColDef} from '@mui/x-data-grid';

export const operadorGridColumns: GridColDef[] = [
    { field: 'nome', headerName: 'Nome', flex: 1, minWidth: 150, resizable: false },
    { field: 'accessKey', headerName: 'Access Key', flex: 1, minWidth: 250, resizable: false },
];