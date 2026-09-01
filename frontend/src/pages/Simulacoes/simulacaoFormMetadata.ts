import type { FormMetadata } from '../../types/formMetadata';

export const simulacaoFormMetadata: FormMetadata = {
    reviewAtTheEnd: false,
    steps: [
        {
            stepNumber: 1,
            title: "Configuração da Simulação",
            rows: [
                {
                    number: 1,
                    subtitle: "Seleção de Ativo",
                    templates: [
                        { sequence: 1, name: "SelecaoParTemplate", width: 12 }
                    ]
                },
                {
                    number: 2,
                    subtitle: "Configuração Operacional",
                    formFields: [
                        { sequence: 1, label: 'Intervalo', field: 'intervalo', type: 'enum', options: ["1m", "5m", "15m", "30m", "1h", "4h", "1d"], fieldSize: '6', nullable: false },
                        { sequence: 2, label: 'Saldo Inicial ($)', field: 'saldoInicial', type: 'double', fieldSize: '6', nullable: false, minValue: 1 }
                    ]
                },
                {
                    number: 3,
                    subtitle: "Estratégia de Execução",
                    templates: [
                        { sequence: 1, name: "SelecaoEstrategiaSimulacaoTemplate", width: 12 }
                    ]
                },
                {
                    number: 4,
                    subtitle: "Período da Simulação",
                    formFields: [
                        { sequence: 1, label: 'Data de Início', field: 'dataInicio', type: 'datetime', fieldSize: '6', nullable: false },
                        { sequence: 2, label: 'Data Fim', field: 'dataFim', type: 'datetime', fieldSize: '6', nullable: false },
                    ]
                }
            ]
        }
    ]
};