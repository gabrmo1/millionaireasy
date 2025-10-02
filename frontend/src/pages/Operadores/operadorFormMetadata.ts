import type { FormMetadata } from '../../types/formMetadata';

export const operadorFormMetadata: FormMetadata = {
    reviewAtTheEnd: false,
    steps: [
        {
            stepNumber: 1,
            title: 'Informações do Operador',
            rows: [
                {
                    number: 1,
                    subtitle: 'Credenciais da Corretora',
                    formFields: [
                        {
                            sequence: 1,
                            label: 'Nome do Operador',
                            field: 'nome',
                            type: 'string',
                            fieldSize: '12',
                            description: 'Dê um nome único para identificar estas credenciais (ex: "Conta Principal MEXC").',
                            nullable: false,
                            maxLength: 50,
                        },
                    ],
                },
                {
                    number: 2,
                    formFields: [
                        {
                            sequence: 2,
                            label: 'Access Key (Chave de Acesso)',
                            field: 'accessKey',
                            type: 'string',
                            fieldSize: '12',
                            description: 'A chave de API fornecida pela sua corretora.',
                            nullable: false,
                            maxLength: 64,
                        },
                    ],
                },
                {
                    number: 3,
                    formFields: [
                        {
                            sequence: 3,
                            label: 'Secret Key (Chave Secreta)',
                            field: 'secretKey',
                            type: 'password',
                            fieldSize: '12',
                            description: 'A chave secreta associada à sua chave de API.',
                            nullable: false,
                            maxLength: 64,
                        },
                    ],
                },
            ],
        },
    ],
};