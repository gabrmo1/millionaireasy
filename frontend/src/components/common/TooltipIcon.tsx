import React from 'react';
import { Tooltip, IconButton } from '@mui/material';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';

interface TooltipIconProps {
    description: string;
}

const TooltipIcon: React.FC<TooltipIconProps> = ({ description }) => {
    return (
        <Tooltip title={description} arrow placement="top">
            <IconButton size="small" sx={{ ml: 0.5 }}>
                <InfoOutlinedIcon fontSize="small" color="primary" />
            </IconButton>
        </Tooltip>
    );
};

export default TooltipIcon;