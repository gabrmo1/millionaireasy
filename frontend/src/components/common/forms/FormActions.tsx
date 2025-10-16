import React from 'react';
import { Box, Button, CircularProgress } from '@mui/material';

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
    const isLastStep = activeStep === totalSteps - 1;

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
                        onClick={isLastStep ? handleSubmit : handleNext}
                        disabled={loading}
                        sx={{ minWidth: '95px' }}
                    >
                        {loading && isLastStep ? (
                            <CircularProgress size={24} color="inherit" />
                        ) : (
                            isLastStep ? 'Salvar' : 'Avançar'
                        )}
                    </Button>
                </Box>
            </Box>
        </Box>
    );
};

export default FormActions;