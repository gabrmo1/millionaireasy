# Roadmap de Desenvolvimento - Millionaireasy Frontend

Este documento descreve o planejamento e as futuras implementações para a interface da aplicação Millionaireasy.

## Fase 1: Refatoração das Telas de Estratégia

**Objetivo:** Alinhar a interface do usuário (UI) com o novo modelo de domínio do backend, focando em uma experiência de criação de estratégias mais unificada e intuitiva.

1.  **Criação da Nova Seção "Estratégias"**
    * [ ] Desenvolver uma nova página de CRUD para a entidade `Estrategia`.
    * [ ] Construir um novo formulário unificado que permita ao usuário configurar, em uma única tela:
        * Os **parâmetros dos indicadores** (ex: períodos de RSI, EMA).
        * Múltiplas **condições de compra** baseadas nesses indicadores.
        * Múltiplas **condições de venda** baseadas nesses indicadores.
    * [ ] A interface deve ser dinâmica, permitindo ao usuário adicionar ou remover condições de forma flexível.

2.  **Atualização da Tela de "Operações"**
    * [ ] Modificar o formulário de `Operacao` para substituir os seletores de configurações por um único seletor da nova `Estrategia`.

3.  **Desativação das Telas Antigas**
    * [ ] Remover do menu e do código as páginas e componentes relacionados a `ConfiguracaoOperacaoAnalise`, `ConfiguracaoOperacaoCompra` e `ConfiguracaoOperacaoVenda`.

## Fases Futuras

* **Dashboard em Tempo Real:** Após a implementação do WebSocket no backend, criar um novo painel que exiba os preços e os valores dos indicadores em tempo real.
* **Histórico de Transações:** Criar uma tela para visualizar o histórico de todas as compras e vendas executadas pelo bot.