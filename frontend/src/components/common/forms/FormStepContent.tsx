import React from 'react';
import { Box, Typography, Divider, Alert } from '@mui/material';
import Grid from '@mui/material/Grid';

import type { FormStepMetadata, FormFieldMetadata } from '../../../types/formMetadata';
import type { TemplateProps } from './DynamicForm';
import FormFieldRenderer from './FormFieldRenderer';

interface FormStepContentProps {
    step: FormStepMetadata;
    templates: Record<string, React.ComponentType<TemplateProps>>;
    formData: Record<string, any>;
    errors: Record<string, string | null>;
    handleChange: (field: string, value: any) => void;
}

const FormStepContent: React.FC<FormStepContentProps> = ({ step, templates, formData, errors, handleChange }) => {

    const isFieldVisible = (field: FormFieldMetadata): boolean => {
        if (field.hidden) return false;

        if (!field.visibleWhen || field.visibleWhen.length === 0) return true;

        return field.visibleWhen.every(condition => {
            const fieldValue = formData[condition.field];
            const targetValue = condition.value;

            switch (condition.operator) {
                case 'eq':
                    return fieldValue == targetValue;
                case 'neq':
                    return fieldValue != targetValue;
                case 'gt':
                    return Number(fieldValue) > Number(targetValue);
                case 'lt':
                    return Number(fieldValue) < Number(targetValue);
                case 'contains':
                    return Array.isArray(fieldValue)
                        ? fieldValue.includes(targetValue)
                        : String(fieldValue || '').includes(String(targetValue));
                default:
                    return true;
            }
        });
    };

    const renderTemplate = (template: { name: string, sequence: number, width: number }) => {
        const TemplateComponent = templates[template.name];
        if (!TemplateComponent) {
            return <Alert severity="error">Template "{template.name}" não encontrado.</Alert>;
        }
        return <TemplateComponent formData={formData} errors={errors} handleChange={handleChange} />;
    };

    return (
        <>
            {step.rows.map(row => (
                <Box key={row.number} sx={{ mt: row.subtitle && row.number > 1 ? 4 : 1.5 }}>
                    {row.subtitle && (
                        <Divider sx={{ mb: 3.5 }}>
                            <Typography variant="subtitle1" sx={{ fontWeight: 500 }}>
                                {row.subtitle}
                            </Typography>
                        </Divider>
                    )}
                    <Grid container spacing={2.5}>
                        {row.formFields?.map(field => {
                            if (!isFieldVisible(field)) return null;
                            return (
                                <Grid size={{ xs: 12, sm: Number(field.fieldSize) }} key={field.sequence}>
                                    <FormFieldRenderer
                                        field={field}
                                        formData={formData}
                                        errors={errors}
                                        handleChange={handleChange}
                                    />
                                </Grid>
                            );
                        })}
                        {row.templates?.map(template => (
                            <Grid size={{ xs: 12, sm: template.width }} key={template.sequence}>
                                {renderTemplate(template)}
                            </Grid>
                        ))}
                    </Grid>
                </Box>
            ))}
        </>
    );
};

export default FormStepContent;