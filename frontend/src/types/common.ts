export interface BaseEntity {
    id: string;
    [key: string]: any;
}

export type FieldType = 'text' | 'number' | 'password' | 'checkbox' | 'entitySelector' | 'select';

export interface ModalConfig<T extends BaseEntity> {
    title: string;
    fetcher: () => Promise<T[]>;
    displayAttribute: keyof T;
}

export interface ValidationRules {
    max?: { value: number; message: string };
    min?: { value: number; message: string };
    pattern?: { value: RegExp; message: string };
    maxLength?: { value: number; message: string };
}

export interface FormField<T extends BaseEntity> {
    name: string;
    label: string;
    type: FieldType;
    required?: boolean;
    defaultValue?: any;
    gridSpan?: number;
    modalConfig?: ModalConfig<T>;
    options?: { value: string; label: string }[];
    validation?: ValidationRules;
    dependentOn?: string; // Nova propriedade
}