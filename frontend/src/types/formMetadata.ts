export type ConditionOperator = 'eq' | 'neq' | 'gt' | 'lt' | 'contains';

export interface FieldCondition {
    field: string;
    operator: ConditionOperator;
    value: any;
}

export interface ArrayConfig {
    itemLabel: string;
    schema: FormFieldMetadata[];
}

export interface FormFieldMetadata {
    sequence: number;
    label: string;
    field: string;
    type: 'string' | 'password' | 'boolean' | 'double' | 'enum' | 'integer' | 'date' | 'datetime' | 'array';
    fieldSize: string;
    description?: string;
    nullable: boolean;
    options?: string[];
    minValue?: number;
    maxValue?: number;
    maxLength?: number;
    hidden?: boolean;
    visibleWhen?: FieldCondition[];
    dependsOn?: string;
    dependentOptions?: Record<string, string[]>;
    validateMatches?: string;
    arrayConfig?: ArrayConfig;
    inputAdornment?: {
        text: string;
        position: 'start' | 'end';
    };
}

export interface TemplateMetadata {
    sequence: number;
    name: string;
    width: number;
}

export interface FormRowMetadata {
    number: number;
    subtitle?: string;
    formFields?: FormFieldMetadata[];
    templates?: TemplateMetadata[];
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