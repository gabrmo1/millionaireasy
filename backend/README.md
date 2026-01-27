# 📘 Documentação do Fluxo de Funcionamento - Millionaireasy
Este documento descreve o ciclo de vida completo do sistema, desde o cadastro manual das entidades administrativas até o processamento automatizado de dados de mercado em tempo real e execução de ordens.

## 1. Configuração Inicial (Setup Manual)
   Antes que o bot possa operar, é necessário configurar as entidades básicas através da API REST (/v1/...).

### Passo 1: Criação do Operador
O Operador representa a conta na exchange (MEXC) que executará as ordens.

Endpoint: 
```
POST /v1/operadores
```

Dados Necessários:

- nome: Identificador interno (ex: "Conta Principal").
- accessKey: Chave pública da API da MEXC.
- secretKey: Chave secreta da API (armazenada com criptografia no banco).

### Passo 2: Definição da Estratégia
A Estratégia é o "cérebro" da operação. Ela define o quê calcular e quando agir.

Endpoint: 
```
POST /v1/estrategias
```

Dados Necessários:

- indicadoresConfig: Lista de indicadores a serem calculados (ex: RSI 14, EMA 200).
- condicoesCompra: Regras lógicas para entrada (ex: RSI < 30 AND Preço > EMA).
- condicoesVenda: Regras lógicas para saída (ex: RSI > 70 OR Lucro > 1%).

valorOperacaoFixo: Quanto investir por ordem (em USDT).

### Passo 3: Criação e Início da Operação
A Operação é a instância ativa que liga uma Estratégia a um Par de Moedas específico usando as credenciais de um Operador.

Endpoint: 
```
POST /v1/operacoes
```
Dados: 
- par (ex: "BTCUSDT"), 
- intervalo (ex: "15m"),
- idOperador, idEstrategia.

Ação de Start: Ao chamar ``` POST /v1/operacoes/{id}/start: ```

O status muda para EM_ANDAMENTO. \
O sistema carrega a operação para o Cache Redis (OperacaoCacheService). \
O MexcSubscriptionService é notificado e envia um comando de SUBSCRIPTION para o WebSocket da MEXC no canal do par/intervalo específico.

## 2. Fluxo de Dados em Tempo Real (Runtime)
Uma vez iniciada a operação, o sistema entra em um ciclo contínuo de processamento de eventos orientado a mensagens.

### 2.1. Ingestão de Dados (WebSocket Client)
Fonte: O MexcWebSocketClient mantém uma conexão persistente com ```wss://wbs-api.mexc.com/ws.```

*Recebimento:*

Recebe mensagens binárias (Protobuf) contendo dados de K-line (Candles) em tempo real.

*Roteamento:*

A mensagem é desserializada de Protobuf para DTO (MexcKlineEventDTO). \
É publicada imediatamente no RabbitMQ (Topic: mexc.data.topic), com entrega Non-Persistent para máxima velocidade.

### 2.2. Processamento e Análise (Consumer)
O KlineAnalysisConsumer consome as mensagens da fila e executa a lógica core:

*Detecção de Virada de Candle (Turnover):*

O sistema compara o horário de fechamento do candle recebido com o que está em cache (Redis). \
Se for um novo candle: Detectamos uma "virada". O candle anterior (fechado) é recuperado para processamento final e persistência no PostgreSQL (Write-Behind). O Throttle (limite de taxa) é ignorado para garantir precisão. \
Se for o mesmo candle: É apenas uma atualização de preço (tick). O sistema aplica um Throttle (ex: 3s) para evitar sobrecarga de CPU desnecessária.

*Gestão de Estado (IndicadorStateService):*

Para indicadores acumulativos (como EMA e RSI), o sistema mantém o estado matemático anterior no Redis. \
Na virada: O estado é "avançado" (advanceState). O valor final do indicador é calculado e salvo como base para o próximo período. \
Durante o tick: O cálculo é feito de forma incremental e volátil, projetando como o indicador ficaria se o candle fechasse naquele instante.

*Cálculo de Indicadores:*

O CalculoIndicadorService processa apenas os indicadores exigidos pelas estratégias ativas naquele par. \
Ex: Se a estratégia pede RSI(14), ele calcula baseando-se no preço atual + histórico acumulado.

*Avaliação de Condições:*

O AvaliacaoCondicaoService compara os valores calculados com as regras da estratégia (ex: RSI_Calculado (28.5) < RSI_Limite (30)?).

### 2.3. Execução de Ordens
Se as condições de Compra forem atendidas:
- O GestaoOrdemService registra uma intenção de compra.
- Uma mensagem é enviada para a fila de execução de ordens (RabbitMQ: orders.actions.topic).
- Se as condições de Venda forem atendidas:

Processo similar, disparando evento de venda.

## 3. Integridade e Performance
O sistema foi arquitetado para resolver problemas comuns em bots de trading:

- Zero Gaps na Virada: A lógica de "Turnover" garante que o sistema nunca pule o processamento do milissegundo exato de fechamento de um candle, essencial para a precisão de indicadores técnicos. 
- Write-Behind: O banco de dados (PostgreSQL) só é acionado quando um candle fecha. As milhares de atualizações de preço por segundo ficam apenas na memória/Redis, protegendo o disco de I/O excessivo. 
- Cálculo Otimizado: Indicadores não são recalculados lendo 200 candles do banco a cada tick. Utilizamos cálculo incremental sobre um estado em cache, reduzindo a complexidade de O(N) para O(1).

## 4. TODO-List
1. Adicionar funções restantes na aplicação
2. Criar funcionalidade de exibir relatórios
3. Criar "spam" de inúmeras operações com diferentes configurações (teste de carga e integridade da aplicação)
4. Criar funcionalidade de realizar backTesting
5. Criar interface para personalização da aplicação
6. Criar conta de admim e funcionalidade para cadastrar novos indicadores e registrar quais fórmulas aquele indicador utilizará