import { Check, Close } from '@mui/icons-material';
import { Box } from '@mui/material';
import type { GridCellParams } from '@mui/x-data-grid';

export const renderBooleanCell = (params: GridCellParams<any, boolean>) => {
    if (params.value == null) {
        return '';
    }

    const boxSx = {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        width: '100%',
        height: '100%',
    };

    return params.value ?
        <Box sx={{ ...boxSx, color: 'success.main' }}>
            <Check />
        </Box> :
        <Box sx={{ ...boxSx, color: 'error.main' }}>
            <Close />
        </Box>;
};