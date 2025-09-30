import React from 'react';
import { Dialog, DialogTitle, DialogContent, IconButton, Box, alpha } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';

interface FormModalProps {
    open: boolean;
    onClose: () => void;
    title: string;
    children: React.ReactNode;
}

const FormModal: React.FC<FormModalProps> = ({ open, onClose, title, children }) => {
    return (
        <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth PaperProps={{
            sx: {
                height: '90vh',
                maxHeight: '900px',
                borderRadius: '16px',
                backgroundColor: (theme) => alpha(theme.palette.background.paper, 0.8),
                backdropFilter: 'blur(12px)',
                border: (theme) => `1px solid ${alpha(theme.palette.text.primary, 0.1)}`,
            }
        }}>
            <DialogTitle sx={{ borderBottom: 1, borderColor: 'divider' }}>
                <Box display="flex" justifyContent="space-between" alignItems="center">
                    {title}
                    <IconButton onClick={onClose}>
                        <CloseIcon />
                    </IconButton>
                </Box>
            </DialogTitle>
            <DialogContent sx={{ p: 0 }}>
                {children}
            </DialogContent>
        </Dialog>
    );
};

export default FormModal;