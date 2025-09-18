import React from 'react';
import {
    TextField, Checkbox, FormControlLabel, FormControl,
    InputLabel, Select, MenuItem, FormHelperText
} from '@mui/material';
import type { FormField } from '../../../types/common';
import EntitySelectorField from './EntitySelectorField';

interface FormFieldRendererProps {
    field: FormField<any>;
    formData: Record<string, any>;
    onChange: (name: string, value: any) => void;
    error?: boolean;
    helperText?: string | null;
}

const FormFieldRenderer: React.FC<FormFieldRendererProps> = ({ field, formData, onChange, error = false, helperText = null }) => {
    const value = formData[field.name];

    switch (field.type) {
        case 'entitySelector':
            return (
                <EntitySelectorField
                    label={field.label}
                    value={value}
                    modalTitle={field.modalConfig!.title}
                    fetcher={field.modalConfig!.fetcher}
                    displayAttribute={field.modalConfig!.displayAttribute}
                    onChange={(id) => onChange(field.name, id)}
                    required={field.required}
                    size="small"
                    error={error}
                    helperText={helperText}
                />
            );
        case 'checkbox':
            return (
                <FormControlLabel
                    control={<Checkbox checked={!!value} onChange={(e) => onChange(field.name, e.target.checked)} name={field.name} size="small" />}
                    label={field.label}
                />
            );
        case 'select':
            return (
                <FormControl fullWidth required={field.required} size="small" error={error}>
                    <InputLabel>{field.label}</InputLabel>
                    <Select
                        name={field.name}
                        label={field.label}
                        value={value ?? ''}
                        onChange={(e) => onChange(field.name, e.target.value)}
                    >
                        {field.options?.map(option => (<MenuItem key={option.value} value={option.value}> {option.label} </MenuItem>))}
                    </Select>
                    <FormHelperText>{helperText || ' '}</FormHelperText>
                </FormControl>
            );
        case 'text':
        case 'number':
        case 'password':
        default:
            return (
                <TextField
                    label={field.label}
                    name={field.name}
                    type={field.type}
                    value={value ?? ''}
                    onChange={(e) => onChange(field.name, e.target.value)}
                    required={field.required}
                    fullWidth
                    size="small"
                    error={error}
                    helperText={helperText || ' '}
                />
            );
    }
};

export default FormFieldRenderer;