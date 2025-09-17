import {
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogContentText,
    DialogTitle,
    Divider
} from '@mui/material';

interface ConfirmationModalProps {
    open: boolean;
    title: string;
    message: string;
    onConfirm: () => void;
    onClose: () => void;
}

export default function ConfirmationModal({ open, title, message, onConfirm, onClose }: ConfirmationModalProps) {
    const handleConfirm = () => {
        onConfirm();
        onClose();
    };

    return (
        <Dialog
            open={open}
            onClose={onClose}
            PaperProps={{
                sx: {
                    borderRadius: 2,
                }
            }}
        >
            <DialogTitle sx={{ fontWeight: 'bold' }}>
                {title}
            </DialogTitle>
            <Divider />
            <DialogContent sx={{ py: 3 }}>
                <DialogContentText>
                    {message}
                </DialogContentText>
            </DialogContent>
            <Divider />
            <DialogActions sx={{ p: 2 }}>
                <Button variant="outlined" onClick={onClose}>
                    Cancelar
                </Button>
                <Button variant="contained" onClick={handleConfirm} color="error" autoFocus>
                    Excluir
                </Button>
            </DialogActions>
        </Dialog>
    );
}