import React from 'react';
import { Box, Collapse } from '@mui/material';
import Grid from '@mui/material/GridLegacy';
import FormFieldRenderer from '../../../components/common/forms/FormFieldRenderer';
import { estrategiaFormConfig } from '../estrategiaConfig';

interface Step2Props {
    formData: any;
    handleMainChange: (name: string, value: any) => void;
}

const Step2_ParametrosAnalise: React.FC<Step2Props> = ({ formData, handleMainChange }) => {
    return (
        <Grid container spacing={1.5}>
            {estrategiaFormConfig.filter(field => field.type === 'checkbox').map(field => {
                const dependentFields = estrategiaFormConfig.filter(depField => depField.dependentOn === field.name);
                return (
                    <Grid item xs={12} key={field.name}>
                        <FormFieldRenderer
                            field={field}
                            formData={formData}
                            onChange={handleMainChange}
                        />
                        {dependentFields.length > 0 && (
                            <Collapse in={!!formData[field.name as keyof typeof formData]} timeout="auto" unmountOnExit>
                                <Box sx={{ pl: 2, pt: 1.5, borderLeft: 2, borderColor: 'divider', ml: 1.5, mt: 1 }}>
                                    <Grid container spacing={1.5}>
                                        {dependentFields.map(depField => (
                                            <Grid item xs={12} sm={depField.gridSpan ?? 12} key={depField.name}>
                                                <FormFieldRenderer
                                                    field={depField}
                                                    formData={formData}
                                                    onChange={handleMainChange}
                                                />
                                            </Grid>
                                        ))}
                                    </Grid>
                                </Box>
                            </Collapse>
                        )}
                    </Grid>
                );
            })}
        </Grid>
    );
};

export default Step2_ParametrosAnalise;