import { Check, Close } from '@mui/icons-material'; // Ícones alterados
import { Box } from '@mui/material';
import type { GridCellParams } from '@mui/x-data-grid';

export const renderBooleanCell = (params: GridCellParams<any, boolean>) => {
    if (params.value == null) {
        return '';
    }

    return params.value ?
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', width: '100%', color: 'success.main' }}>
            <Check />
        </Box> :
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', width: '100%', color: 'error.main' }}>
            <Close />
        </Box>;
};