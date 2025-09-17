import type {GridColDef} from '@mui/x-data-grid';
import type {FormField} from '../../types/common';

export const operadorGridColumns: GridColDef[] = [
    { field: 'nome', headerName: 'Nome', flex: 1, minWidth: 150 },
    { field: 'accessKey', headerName: 'Access Key', flex: 1, minWidth: 250 },
];

export const operadorFormConfig: FormField<any>[] = [
    {
        name: 'nome', label: 'Nome do Operador', type: 'text', required: true, gridSpan: 4,
        validation: { maxLength: { value: 50, message: 'O nome não pode exceder 50 caracteres.' } }
    },
    {
        name: 'accessKey', label: 'Access Key (Chave de Acesso)', type: 'text', required: true, gridSpan: 4,
        validation: { maxLength: { value: 64, message: 'A Access Key não pode exceder 64 caracteres.' } }
    },
    {
        name: 'secretKey', label: 'Secret Key (Chave Secreta)', type: 'password', required: true, gridSpan: 4,
        validation: { maxLength: { value: 64, message: 'A Secret Key não pode exceder 64 caracteres.' } }
    },
];