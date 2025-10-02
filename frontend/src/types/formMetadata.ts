export interface FormFieldMetadata {
    sequence: number;
    label: string;
    field: string;
    type: 'string' | 'password' | 'boolean' | 'double' | 'enum' | 'integer';
    fieldSize: string; // Ex: "6", "12"
    description?: string;
    nullable: boolean;
    options?: string[];
    minValue?: number;
    maxValue?: number;
    maxLength?: number;
    inputAdornment?: {
        text: string;
        position: 'start' | 'end';
    };
}

export interface FormRowMetadata {
    number: number;
    subtitle?: string;
    formFields?: FormFieldMetadata[];
    templates?: any[]; // Para templates customizados no futuro
}

export interface FormStepMetadata {
    stepNumber: number;
    title: string;
    rows: FormRowMetadata[];
}

export interface FormMetadata {
    reviewAtTheEnd: boolean;
    steps: FormStepMetadata[];
}