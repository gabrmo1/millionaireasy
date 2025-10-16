import React, { useState } from 'react';
import {
    TextField,
    InputAdornment,
    IconButton,
    FormControl,
    InputLabel,
    Select,
    MenuItem,
    FormHelperText,
    FormControlLabel,
    Checkbox
} from '@mui/material';
import { Visibility, VisibilityOff } from '@mui/icons-material';
import type { FormFieldMetadata } from '../../../types/formMetadata';
import { formatLeadingZeros } from "../../../utils/inputFormatters.ts";

interface FormFieldRendererProps {
    field: FormFieldMetadata;
    formData: Record<string, any>;
    errors: Record<string, string | null>;
    handleChange: (field: string, value: any) => void;
}

const FormFieldRenderer: React.FC<FormFieldRendererProps> = ({ field, formData, errors, handleChange }) => {
    const [showPassword, setShowPassword] = useState(false);

    if (field.hidden) return null;

    const commonProps = {
        key: field.field,
        label: field.label,
        name: field.field,
        required: !field.nullable,
        error: !!errors[field.field],
        size: 'small' as const,
        fullWidth: true,
    };

    const processDynamicText = (text: string): string => {
        return text.replace(/\${(.*?)}/g, (_, key) => formData[key] || '');
    };

    const handleNumericChange = (value: string) => {
        const formattedValue = formatLeadingZeros(value.replace(/[^0-9.]/g, ''));
        handleChange(field.field, formattedValue);
    };

    const adornment = field.inputAdornment ? (
        <InputAdornment position={field.inputAdornment.position}>
            {processDynamicText(field.inputAdornment.text)}
        </InputAdornment>
    ) : null;

    switch (field.type) {
        case 'password':
            return (
                <TextField
                    {...commonProps}
                    type={showPassword ? 'text' : 'password'}
                    value={formData[field.field] ?? ''}
                    onChange={(e) => handleChange(field.field, e.target.value)}
                    helperText={errors[field.field] || field.description || ' '}
                    InputProps={{
                        endAdornment: (
                            <InputAdornment position="end">
                                <IconButton
                                    onClick={() => setShowPassword(!showPassword)}
                                    onMouseDown={(e) => e.preventDefault()}
                                    edge="end"
                                >
                                    {showPassword ? <VisibilityOff /> : <Visibility />}
                                </IconButton>
                            </InputAdornment>
                        ),
                    }}
                />
            );
        case 'double':
        case 'integer':
            return (
                <TextField
                    {...commonProps}
                    type="number"
                    value={formData[field.field] ?? ''}
                    onChange={(e) => handleNumericChange(e.target.value)}
                    helperText={errors[field.field] || processDynamicText(field.description || ' ')}
                    inputProps={{ min: field.minValue, max: field.maxValue }}
                    InputProps={{
                        [field.inputAdornment?.position === 'start' ? 'startAdornment' : 'endAdornment']: adornment
                    }}
                />
            );
        case 'enum':
            return (
                <FormControl {...commonProps}>
                    <InputLabel>{field.label}</InputLabel>
                    <Select
                        label={field.label}
                        value={formData[field.field] ?? ''}
                        onChange={(e) => handleChange(field.field, e.target.value)}
                    >
                        {field.options?.map(opt => <MenuItem key={opt} value={opt}>{opt}</MenuItem>)}
                    </Select>
                    <FormHelperText>{errors[field.field] || field.description || ' '}</FormHelperText>
                </FormControl>
            );
        case 'boolean':
            return (
                <FormControlLabel
                    control={
                        <Checkbox
                            checked={!!formData[field.field]}
                            onChange={(e) => handleChange(field.field, e.target.checked)}
                            name={field.field}
                            size="small"
                        />
                    }
                    label={field.label}
                />
            );
        case 'string':
        default:
            return (
                <TextField
                    {...commonProps}
                    type="text"
                    value={formData[field.field] ?? ''}
                    onChange={(e) => handleChange(field.field, e.target.value)}
                    helperText={errors[field.field] || field.description || ' '}
                />
            );
    }
};

export default FormFieldRenderer;