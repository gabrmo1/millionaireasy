import React from 'react';
import { Box, Button } from '@mui/material';

interface FormActionsProps {
    activeStep: number;
    totalSteps: number;
    loading: boolean;
    onClose: () => void;
    handleBack: () => void;
    handleNext: () => void;
    handleSubmit: () => void;
}

const FormActions: React.FC<FormActionsProps> = ({
                                                     activeStep,
                                                     totalSteps,
                                                     loading,
                                                     onClose,
                                                     handleBack,
                                                     handleNext,
                                                     handleSubmit
                                                 }) => {
    return (
        <Box sx={{ p: 2, borderTop: 1, borderColor: 'divider' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                <Button variant="outlined" onClick={onClose} disabled={loading}>
                    Cancelar
                </Button>
                <Box>
                    {totalSteps > 1 && (
                        <Button
                            color="inherit"
                            disabled={activeStep === 0 || loading}
                            onClick={handleBack}
                            sx={{ mr: 1 }}
                        >
                            Voltar
                        </Button>
                    )}
                    <Button
                        variant="contained"
                        onClick={activeStep === totalSteps - 1 ? handleSubmit : handleNext}
                        disabled={loading}
                    >
                        {activeStep === totalSteps - 1 ? 'Salvar' : 'Avançar'}
                    </Button>
                </Box>
            </Box>
        </Box>
    );
};

export default FormActions;