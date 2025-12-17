import type { FormMetadata } from '../../types/formMetadata';

export const operacaoFormMetadata: FormMetadata = {
    reviewAtTheEnd: true,
    steps: [
        {
            stepNumber: 1,
            title: "Seleção do Par",
            rows: [
                {
                    number: 1,
                    formFields: [
                        { sequence: 1, label: 'Par', field: 'par', type: 'string', fieldSize: '12', nullable: false, hidden: true }
                    ],
                    templates: [
                        { sequence: 1, name: "SelecaoParTemplate", width: 12 }
                    ]
                }
            ]
        },
        {
            stepNumber: 2,
            title: "Configuração",
            rows: [
                {
                    number: 1,
                    formFields: [
                        { sequence: 1, label: 'Intervalo', field: 'intervalo', type: 'string', fieldSize: '12', nullable: false, hidden: true },
                        { sequence: 2, label: 'Operador', field: 'idOperador', type: 'string', fieldSize: '12', nullable: true, hidden: true },
                        { sequence: 3, label: 'Estratégia', field: 'idEstrategia', type: 'string', fieldSize: '12', nullable: true, hidden: true },
                        { sequence: 4, label: 'Modo Teste', field: 'modoTeste', type: 'boolean', fieldSize: '12', nullable: false, hidden: true },
                        { sequence: 5, label: 'Saldo Inicial', field: 'saldoInicial', type: 'double', fieldSize: '12', nullable: true, hidden: true }
                    ],
                    templates: [
                        { sequence: 1, name: "ConfiguracaoOperacaoTemplate", width: 12 }
                    ]
                }
            ]
        },
        {
            stepNumber: 3,
            title: "Revisão",
            rows: [
                {
                    number: 1,
                    templates: [
                        { sequence: 1, name: "RevisaoOperacaoTemplate", width: 12 }
                    ]
                }
            ]
        }
    ]
};