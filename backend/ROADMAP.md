# Roadmap de Desenvolvimento - Millionaireasy

Este documento descreve o planejamento e as futuras implementações para a evolução da aplicação Millionaireasy, seguindo uma abordagem de "Modelagem Primeiro".

## Fase 1: Refatoração do Modelo de Domínio e Estratégias

**Objetivo:** Estabelecer a nova estrutura de dados para `Estrategias` como a base para todo o sistema, eliminando redundâncias e simplificando o core da aplicação.

1.  **Criação das Novas Entidades de Domínio**
    * [ ] Criar a entidade `Estrategia` para ser a fonte única da verdade dos parâmetros de indicadores (períodos de RSI, EMA, etc.).
    * [ ] Criar a entidade `CondicaoCompra` para definir regras de gatilho de compra, associada a uma `Estrategia`.
    * [ ] Criar a entidade `CondicaoVenda` para definir regras de gatilho de venda, associada a uma `Estrategia`.

2.  **Refatoração do Core**
    * [ ] Refatorar a entidade `Operacao` para remover as associações antigas e linkar diretamente a uma `Estrategia`.
    * [ ] Atualizar todos os Services, Repositories e DTOs para refletir o novo modelo.
    * [ ] Remover completamente as entidades e classes relacionadas a `ConfiguracaoOperacaoAnalise`, `ConfiguracaoOperacaoCompra` e `ConfiguracaoOperacaoVenda`.

## Fase 2: Conteinerização do Ambiente (Docker & Docker Compose)

**Objetivo:** Simplificar a configuração do ambiente de desenvolvimento, garantir consistência entre os ambientes e preparar a aplicação para a nuvem.

1.  **Dockerização da Aplicação**
    * [ ] Criar um `Dockerfile` na raiz do projeto para empacotar a aplicação Spring Boot em uma imagem Docker.
2.  **Orquestração com Docker Compose**
    * [ ] Criar um arquivo `docker-compose.yml`.
    * [ ] Adicionar e configurar os serviços essenciais:
        * `app`: Nosso backend Spring Boot.
        * `postgres`: O banco de dados.
        * `rabbitmq`: O message broker para a Fase 3.
        * `redis`: Para gerenciamento de cache futuro (opcional).
    * [ ] Configurar a aplicação (`application.yml`) para se conectar aos serviços do Docker Compose usando variáveis de ambiente.

## Fase 3: Migração para Arquitetura Orientada a Eventos

**Objetivo:** Implementar a captura de dados em tempo real da MEXC, substituindo o modelo de polling para atingir alta escalabilidade e eficiência.

1.  **Implementação do Serviço de Ingestão de Dados (Data Ingester)**
    * [ ] Desenvolver um cliente WebSocket para se conectar ao endpoint público da MEXC.
    * [ ] Implementar a lógica de inscrição (`SUBSCRIPTION`) para os canais de K-line.
    * [ ] Criar a lógica para detectar o fechamento de um candle e publicar o evento na fila do RabbitMQ.
2.  **Implementação do Serviço de Análise (Consumer)**
    * [ ] Criar um serviço "consumer" do RabbitMQ.
    * [ ] Ao receber um evento de candle fechado, o serviço irá buscar as `Operacoes` ativas, calcular os indicadores conforme definido na `Estrategia`, e verificar se alguma `CondicaoCompra` ou `CondicaoVenda` foi atendida.

## Fase 4: Implementação do Módulo de Execução de Ordens

**Objetivo:** Habilitar o bot para executar ordens de compra e venda reais na exchange.

* [ ] Desenvolver a lógica de autenticação e assinatura de requisições para a API privada da MEXC.
* [ ] Criar um `Serviço de Execução` que consumirá eventos de ordem do RabbitMQ e os enviará para a exchange.
* [ ] Implementar o registro de transações no banco de dados.