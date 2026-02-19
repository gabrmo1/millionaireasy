import React, { useState, useEffect } from 'react';
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
import { DatePicker, DateTimePicker } from '@mui/x-date-pickers';
import { Visibility, VisibilityOff } from '@mui/icons-material';
import dayjs, { Dayjs } from 'dayjs';
import type { FormFieldMetadata } from '../../../types/formMetadata';
import { formatLeadingZeros } from "../../../utils/inputFormatters.ts";
import ArrayFieldRenderer from './ArrayFieldRenderer';

interface FormFieldRendererProps {
    field: FormFieldMetadata;
    formData: Record<string, any>;
    errors: Record<string, string | null>;
    handleChange: (field: string, value: any) => void;
}

const FormFieldRenderer: React.FC<FormFieldRendererProps> = ({ field, formData, errors, handleChange }) => {
    const [showPassword, setShowPassword] = useState(false);
    const parentValue = field.dependsOn ? formData[field.dependsOn] : undefined;
    const getOptions = (): string[] => {
        if (!field.dependsOn) {
            return field.options || [];
        }
        if (parentValue === undefined || parentValue === '' || parentValue === null) {
            return [];
        }
        const parentKey = String(parentValue);
        return field.dependentOptions?.[parentKey] || [];
    };

    const activeOptions = getOptions();

    useEffect(() => {
        if (field.dependsOn && formData[field.field]) {
            const currentOpts = getOptions();
            const currentValue = formData[field.field];
            if (!currentOpts.includes(currentValue)) {
                handleChange(field.field, '');
            }
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [parentValue, field.dependsOn, field.dependentOptions]);

    if (field.hidden) return null;

    const commonProps = {
        label: field.label,
        name: field.field,
    };

    const hasError = !!errors[field.field];
    const errorMessage = errors[field.field] || field.description || ' ';

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
        case 'array':
            return (
                <ArrayFieldRenderer
                    field={field}
                    value={formData[field.field]}
                    onChange={(newValue) => handleChange(field.field, newValue)}
                    error={errors[field.field]}
                />
            );

        case 'enum':
            return (
                <FormControl
                    error={hasError}
                    fullWidth
                    size="small"
                    required={!field.nullable}
                    disabled={!!field.dependsOn && !parentValue}
                >
                    <InputLabel>{field.label}</InputLabel>
                    <Select
                        label={field.label}
                        value={formData[field.field] ?? ''}
                        onChange={(e) => handleChange(field.field, e.target.value)}
                    >
                        {activeOptions.map(opt => <MenuItem key={opt} value={opt}>{opt}</MenuItem>)}
                    </Select>
                    <FormHelperText>{errorMessage}</FormHelperText>
                </FormControl>
            );

        case 'date':
            return (
                <DatePicker
                    {...commonProps}
                    value={formData[field.field] ? dayjs(formData[field.field]) : null}
                    onChange={(newValue: Dayjs | null) => handleChange(field.field, newValue ? newValue.toISOString() : null)}
                    slotProps={{
                        textField: {
                            size: 'small',
                            fullWidth: true,
                            error: hasError,
                            helperText: errorMessage,
                            required: !field.nullable
                        }
                    }}
                />
            );

        case 'datetime':
            return (
                <DateTimePicker
                    {...commonProps}
                    value={formData[field.field] ? dayjs(formData[field.field]) : null}
                    onChange={(newValue: Dayjs | null) => handleChange(field.field, newValue ? newValue.toISOString() : null)}
                    slotProps={{
                        textField: {
                            size: 'small',
                            fullWidth: true,
                            error: hasError,
                            helperText: errorMessage,
                            required: !field.nullable
                        }
                    }}
                    ampm={false}
                />
            );

        case 'password':
            return (
                <TextField
                    {...commonProps}
                    required={!field.nullable}
                    error={hasError}
                    size="small"
                    fullWidth
                    type={showPassword ? 'text' : 'password'}
                    value={formData[field.field] ?? ''}
                    onChange={(e) => handleChange(field.field, e.target.value)}
                    helperText={errorMessage}
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
                    required={!field.nullable}
                    error={hasError}
                    size="small"
                    fullWidth
                    type="number"
                    value={formData[field.field] ?? ''}
                    onChange={(e) => handleNumericChange(e.target.value)}
                    helperText={processDynamicText(errorMessage)}
                    inputProps={{ min: field.minValue, max: field.maxValue }}
                    InputProps={{
                        [field.inputAdornment?.position === 'start' ? 'startAdornment' : 'endAdornment']: adornment
                    }}
                />
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
                    required={!field.nullable}
                    error={hasError}
                    size="small"
                    fullWidth
                    type="text"
                    value={formData[field.field] ?? ''}
                    onChange={(e) => handleChange(field.field, e.target.value)}
                    helperText={errorMessage}
                />
            );
    }
};

export default FormFieldRenderer;