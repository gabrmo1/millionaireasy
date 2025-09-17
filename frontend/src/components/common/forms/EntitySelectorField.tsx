import { useState, useEffect } from 'react';
import { TextField, Button, InputAdornment } from '@mui/material';
import EntitySelectorModal from '../modals/EntitySelectorModal';
import type {BaseEntity} from '../../../types/common';

interface EntitySelectorFieldProps<T extends BaseEntity> {
    label: string;
    value: string | null;
    modalTitle: string;
    fetcher: () => Promise<T[]>;
    displayAttribute: keyof T;
    onChange: (id: string | null) => void;
    required?: boolean;
    error?: boolean;
    helperText?: string | null;
    helperTextError?: boolean;
    size?: 'small' | 'medium';
}

export default function EntitySelectorField<T extends BaseEntity>({
                                                                      label,
                                                                      value,
                                                                      modalTitle,
                                                                      fetcher,
                                                                      displayAttribute,
                                                                      onChange,
                                                                      required = false,
                                                                      error = false,
                                                                      helperText = null,
                                                                      helperTextError = false,
                                                                      size = 'medium'
                                                                  }: EntitySelectorFieldProps<T>) {
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [displayValue, setDisplayValue] = useState('');

    useEffect(() => {
        if (value) {
            fetcher().then(entities => {
                const selected = entities.find(e => e.id === value);
                if (selected) {
                    setDisplayValue(String(selected[displayAttribute]));
                }
            });
        } else {
            setDisplayValue('');
        }
    }, [value, fetcher, displayAttribute]);

    const handleSelect = (entity: T) => {
        onChange(entity.id);
        setDisplayValue(String(entity[displayAttribute]));
    };

    return (
        <>
            <TextField
                label={label}
                value={displayValue}
                fullWidth
                required={required}
                error={error}
                size={size}
                helperText={helperText || ' '}
                FormHelperTextProps={{ style: { color: helperTextError ? '#d32f2f' : undefined } }}
                InputProps={{
                    readOnly: true,
                    endAdornment: (
                        <InputAdornment position="end">
                            <Button onClick={() => setIsModalOpen(true)}>Selecionar</Button>
                        </InputAdornment>
                    ),
                }}
            />

            {isModalOpen && (
                <EntitySelectorModal
                    open={isModalOpen}
                    title={modalTitle}
                    fetcher={fetcher}
                    displayAttribute={displayAttribute}
                    onClose={() => setIsModalOpen(false)}
                    onSelect={handleSelect}
                />
            )}
        </>
    );
}