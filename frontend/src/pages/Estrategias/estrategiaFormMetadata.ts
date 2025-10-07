import type { FormMetadata } from '../../types/formMetadata';

export const estrategiaFormMetadata: FormMetadata = {
    reviewAtTheEnd: true,
    steps: [
        {
            stepNumber: 1,
            title: "Informações",
            rows: [
                {
                    number: 1,
                    formFields: [
                        {
                            sequence: 1,
                            label: "Nome da Estratégia",
                            field: "nome",
                            type: "string",
                            fieldSize: "6",
                            description: "Dê um nome único e descritivo para sua estratégia.",
                            nullable: false
                        },
                        {
                            sequence: 2,
                            label: "Stablecoin Padrão",
                            field: "stablecoin",
                            type: "enum",
                            options: ["USDT", "USDC", "EUR"],
                            fieldSize: "6",
                            description: "Moeda base para operações de compra e lucro.",
                            nullable: false
                        }
                    ]
                }
            ]
        },
        {
            stepNumber: 2,
            title: "Indicadores",
            rows: [
                {
                    number: 1,
                    subtitle: "Indicadores",
                    templates: [
                        {
                            sequence: 1,
                            name: "AdicionarIndicadoresTemplate",
                            width: 12
                        }
                    ]
                }
            ]
        },
        {
            stepNumber: 3,
            title: "Regras de compra",
            rows: [
                {
                    number: 1,
                    subtitle: "Investimento",
                    formFields: [
                        {
                            sequence: 1,
                            label: "Valor Fixo por Operação",
                            field: "valorOperacaoFixo",
                            type: "double",
                            minValue: 0,
                            fieldSize: "6",
                            inputAdornment: {
                                text: "${stablecoin}",
                                position: "end"
                            },
                            description: " ",
                            nullable: true
                        },
                        {
                            sequence: 2,
                            label: "% do Saldo por Operação",
                            field: "percentualValorOperacao",
                            type: "double",
                            minValue: 0,
                            maxValue: 100,
                            fieldSize: "6",
                            inputAdornment: {
                                text: "% ${stablecoin}",
                                position: "end"
                            },
                            description: " ",
                            nullable: true
                        }
                    ]
                },
                {
                    number: 2,
                    subtitle: "Configurações de Compra",
                    templates: [
                        {
                            sequence: 1,
                            name: "AdicionarRegrasCompraTemplate",
                            width: 12
                        }
                    ]
                }
            ]
        },
        {
            stepNumber: 4,
            title: "Regras de Venda",
            rows: [
                {
                    number: 1,
                    subtitle: "Parâmetros de Venda",
                    formFields: [
                        {
                            sequence: 1,
                            label: "Venda por Take Profit (Lucro)",
                            field: "vendaApenasPorLucro",
                            type: "boolean",
                            fieldSize: "12",
                            nullable: true
                        },
                        {
                            sequence: 2,
                            label: "% de Lucro para Venda",
                            field: "percentualLucro",
                            type: "double",
                            fieldSize: "6",
                            nullable: true,
                            description: " "
                        }
                    ]
                },
                {
                    number: 2,
                    subtitle: "Condições de Venda",
                    templates: [
                        {
                            sequence: 1,
                            name: "AdicionarRegrasVendaTemplate",
                            width: 12
                        }
                    ]
                }
            ]
        }
    ]
};