# 🏛️ Plano de Ação: Arquitetura e Implementação do Monólito Modular
## Projeto: Millionaireasy (Trading Bot MEXC)

Este documento estabelece o guia passo a passo para a reestruturação e consolidação do **Millionaireasy** em uma arquitetura de **Monólito Modular** de alta performance. 

O objetivo é manter a separação rigorosa de domínios (Market Engine, OMS/Risco, Ingestão Time-Series, Gestão de Estratégias e BFF), eliminando a latência de rede entre a geração de sinais e a execução de ordens, otimizando o consumo de hardware e acelerando o lançamento do produto no mercado.

---

## 1. Visão Geral da Arquitetura Alvo

### 1.1 Diagrama de Componentes e Fluxo de Dados

```mermaid
flowchart TB
    subgraph Client ["Frontend SPA (Vite + React)"]
        UI["Dashboard, Gráficos & Controle de Robôs"]
    end

    subgraph Host ["Monólito Modular (Spring Boot 3.5.x / Java 21)"]
        direction TB

        subgraph BFFModule ["Módulo BFF & Gateway (`modules.gateway`)"]
            SECURITY["Spring Security (JWT)"]
            WS_BROKER["WebSocket / SSE Controller (Push para UI)"]
            REST_GW["REST Endpoints Protegidos"]
        end

        subgraph StrategyModule ["Módulo Gestão & Estratégia (`modules.strategy`)"]
            CRUD["Operações, Operadores, Estratégias (JPA)"]
            BACKTEST["Engine de Backtesting / Simulação"]
        end

        subgraph MarketModule ["Módulo Market Data (`modules.market`)"]
            WS_MEXC_PUB["WebSocket Público MEXC (Protobuf K-Lines)"]
            L1_CACHE["L1 Cache (Caffeine - Heap JVM)"]
            INDICATORS["Cálculo Incremental O(1) (RSI, EMA, etc.)"]
            COND_EVAL["Avaliação de Condições de Entrada/Saída"]
        end

        subgraph OMSModule ["Módulo OMS & Risco (`modules.oms`)"]
            RISK_GUARD["Gestão de Risco Pré-Trade"]
            RATE_LIMITER["Token Bucket (Bucket4j - Quota MEXC)"]
            ORDER_DISPATCH["REST Client Assinado (HMAC-SHA256 + clientOrderId)"]
            WS_MEXC_PRIV["WebSocket Privado MEXC (User Stream)"]
            KILL_SWITCH["Kill Switch Global (Pânico / Parada de Emergência)"]
        end

        subgraph TimeseriesModule ["Módulo Time-Series (`modules.timeseries`)"]
            BATCH_BUFFER["Buffer de Ingestão em Lote"]
            JDBC_INGEST["Spring Data JDBC / JdbcTemplate.batchUpdate"]
        end

        %% Comunicação Interna
        REST_GW --> CRUD
        WS_MEXC_PUB --> L1_CACHE --> INDICATORS --> COND_EVAL
        
        %% Sinais em Memória (Zero Latency)
        COND_EVAL ==>|In-Memory Event (ApplicationEventPublisher)| RISK_GUARD
        RISK_GUARD --> RATE_LIMITER --> ORDER_DISPATCH
        
        %% Notificações e Confirmações
        WS_MEXC_PRIV -->|Execuções| WS_BROKER
        COND_EVAL -.->|Candles Fechados (RabbitMQ / Queue)| BATCH_BUFFER
        WS_BROKER --> UI
    end

    subgraph DataLayer ["Camada de Persistência e Cache"]
        REDIS[("Redis 7<br/>• L2 Cache (Indicadores)<br/>• Sessões & Rate Limits")]
        PG_TIMESCALEDB[("PostgreSQL 16 + TimescaleDB<br/>• Schema Relacional: Usuários, Robôs, Estratégias<br/>• Hypertables: Candles e Ticks")]
    end

    %% Integrações Externas e de Dados
    UI <==>|WSS / HTTPS| BFFModule
    CRUD --> PG_TIMESCALEDB
    L1_CACHE <--> REDIS
    BATCH_BUFFER --> JDBC_INGEST --> PG_TIMESCALEDB
    ORDER_DISPATCH ==>|REST Assinado| MEXC_REST["MEXC API (Spot/V3)"]
    MEXC_REST -.->|Feed Privado| WS_MEXC_PRIV
```

---

## 2. Mapeamento de Migração (Estrutura Atual ➔ Nova Estrutura)

Atualmente, o projeto está estruturado em pacotes técnicos (`services`, `controllers`, `consumers`, etc.). A migração organiza essas classes em módulos funcionais isolados com contratos públicos:

| Módulo Alvo | Classes Atuais a Mover / Consolidar | Responsabilidade Exclusiva |
| :--- | :--- | :--- |
| **`modules.identity`** | `security/*`, `configs/SecurityConfig`, `controllers/AuthController` | Autenticação, gestão de tokens JWT e criptografia Jasypt das chaves de API. |
| **`modules.strategy`** | `Operador*`, `Estrategia*`, `Operacoes*`, `Simulacao*`, `RelatorioDesempenho*` | CRUDs das entidades do usuário, regras lógicas e relatórios transacionais (JPA). |
| **`modules.market`** | `MexcConnectionService`, `MexcWebSocketClient`, `CalculoIndicadorService`, `IndicadorStateService`, `AvaliacaoCondicaoService`, `KlineAnalysisConsumer` | Ingestão WebSocket pública, cache L1/L2 e cálculo de indicadores técnicos em $O(1)$. |
| **`modules.oms`** | `GestaoOrdemService`, `OrderExecutionConsumer` (expandido para API real), `MexcAuthenticatedIntegration` | Validação de risco pré-trade, respeito a rate limit, envio assinado de ordens e WebSocket privado. |
| **`modules.timeseries`** | `Candle*`, `CandlePersistConsumer`, `BacktestCandleProviderService` | Persistência em lote via JDBC e consultas analíticas de alta performance sobre Hypertables. |
| **`modules.gateway`** | Endpoints REST públicos, Handlers WebSocket / SSE para o frontend SPA | BFF para comunicação bidirecional com o frontend. |

---

## 3. Passo a Passo Detalhado de Implementação

### FASE 1: Preparação do Ambiente e Dependências

#### 1.1 Atualizar `backend/build.gradle.kts`
Adicionar o **Spring Modulith** (para verificação de integridade dos módulos) e o **Bucket4j** (para controle de taxa de requisições à MEXC):

```kotlin
// Em dependencies { ... }
implementation("org.springframework.modulith:spring-modulith-starter-core:1.3.1")
testImplementation("org.springframework.modulith:spring-modulith-starter-test:1.3.1")
implementation("com.github.vladimir-bukhtoyarov:bucket4j-core:7.6.0")
implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
```

#### 1.2 Atualizar o `docker-compose.yml` para TimescaleDB
Substituir a imagem do PostgreSQL padrão pela imagem oficial do TimescaleDB no mesmo serviço:

```yaml
services:
  db:
    image: timescale/timescaledb:latest-pg16
    container_name: millionaireasy-db
    ports:
      - "5432:5432"
    environment:
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=Mexc123!
      - POSTGRES_DB=mexc
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - millionaireasy-net
```

---

### FASE 2: Reestruturação de Pacotes e Módulos de Domínio

Organizar o pacote base `br.com.bot_mexc` na seguinte topologia:

```
br.com.bot_mexc
├── Application.java
├── shared/                         # DTOs compartilhados, Enums e Exceções globais
│   ├── enums/ (TipoOrdem, StatusOperacao, etc.)
│   └── events/ (TradeSignalEvent, OrderFilledEvent, etc.)
├── modules/
│   ├── identity/
│   ├── strategy/
│   ├── market/
│   ├── oms/
│   ├── timeseries/
│   └── gateway/
```

#### Regras Rígidas de Módulos (Enforced via Spring Modulith):
1. **Regra de Ouro:** Um módulo **NUNCA** injeta o `Repository` JPA ou entidades de outro módulo.
2. Cada módulo expõe apenas classes públicas em sua raiz (ou subpacote `api`) para serem usadas por outros módulos.
3. Comunicações assíncronas ou de alta velocidade utilizam eventos de domínio (`shared.events`).

---

### FASE 3: Barramento Interno In-Memory (Zero Latency Trading)

#### 3.1 Criar o Evento de Domínio de Sinal de Trade
Substituir o envio de ordens via fila RabbitMQ entre análise e OMS por um evento em memória:

```java
package br.com.bot_mexc.shared.events;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeSignalEvent(
    Long idOperacao,
    Long idOperador,
    String par,
    String intervalo,
    String tipoOrdem, // COMPRA / VENDA
    BigDecimal precoAtual,
    BigDecimal quantidade,
    BigDecimal stopLoss,
    BigDecimal takeProfit,
    Instant timestamp
) {}
```

#### 3.2 Disparo no Módulo `market` (`AvaliacaoCondicaoService`)
```java
// Em vez de rabbitTemplate.convertAndSend(ORDERS_QUEUE, ...):
applicationEventPublisher.publishEvent(new TradeSignalEvent(...));
```

#### 3.3 Consumo Imediato no Módulo `oms`
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class TradeSignalListener {

    private final PreTradeRiskService riskService;
    private final OrderDispatchService dispatchService;

    @EventListener
    public void onTradeSignal(TradeSignalEvent signal) {
        log.info("[ZERO-LATENCY] Sinal recebido em memória para {}: {}", signal.par(), signal.tipoOrdem());
        if (riskService.validarRisco(signal)) {
            dispatchService.executarOrdem(signal);
        }
    }
}
```
> **Ganho:** Latência reduzida de ~5ms (RabbitMQ) para **< 0.05ms**, eliminando riscos de slippage em momentos de alta volatilidade.

---

### FASE 4: Otimização de Time-Series e TimescaleDB

#### 4.1 Migration Flyway: Ativar TimescaleDB e Hypertables
Criar o arquivo `V1_X__setup_timescaledb.sql` em `src/main/resources/db/migration/`:

```sql
-- 1. Habilitar a extensão
CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;

-- 2. Converter tabela de candles existente em Hypertable particionada por tempo
SELECT create_hypertable('candles', 'tempo_fechamento', if_not_exists => TRUE);

-- 3. Ativar política de compressão nativa (redução de até 90% do disco)
ALTER TABLE candles SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'par, intervalo',
    timescaledb.compress_orderby = 'tempo_fechamento DESC'
);

-- 4. Comprimir automaticamente dados mais antigos que 7 dias
SELECT add_compression_policy('candles', INTERVAL '7 days', if_not_exists => TRUE);
```

#### 4.2 Ingestão em Lote via `JdbcTemplate` no `modules.timeseries`
Substituir o `candleRepository.saveAll(entidades)` do Hibernate por `batchUpdate` direto:

```java
@Service
@RequiredArgsConstructor
public class CandleJdbcBatchService {

    private final JdbcTemplate jdbcTemplate;

    public void batchInsertCandles(List<CandleDTO> candles, String par, String intervalo) {
        String sql = """
            INSERT INTO candles (par, intervalo, tempo_abertura, tempo_fechamento, abertura, fechamento, maximo, minimo, volume)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (par, intervalo, tempo_fechamento) DO UPDATE 
            SET fechamento = EXCLUDED.fechamento, maximo = EXCLUDED.maximo, minimo = EXCLUDED.minimo, volume = EXCLUDED.volume
        """;

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                CandleDTO c = candles.get(i);
                ps.setString(1, par);
                ps.setString(2, intervalo);
                ps.setTimestamp(3, Timestamp.from(c.tempoAbertura()));
                ps.setTimestamp(4, Timestamp.from(c.tempoFechamento()));
                ps.setBigDecimal(5, c.abertura());
                ps.setBigDecimal(6, c.fechamento());
                ps.setBigDecimal(7, c.maximo());
                ps.setBigDecimal(8, c.minimo());
                ps.setBigDecimal(9, c.volume());
            }

            @Override
            public int getBatchSize() {
                return candles.size();
            }
        });
    }
}
```

---

### FASE 5: Implementação Completa do OMS & Gestão de Risco

Preenchimento do `// TODO: Chamada HTTP para MEXC` existente no [OrderExecutionConsumer.java](file:///c:/Users/Gabriel/workspace/millionaireasy/backend/src/main/java/br/com/bot_mexc/consumers/OrderExecutionConsumer.java#L40):

#### 5.1 Rate Limiting por Token Bucket (Bucket4j)
Proteger os limites de chamada da API da MEXC (evitando HTTP 429 e bloqueio de IP):

```java
@Component
public class MexcRateLimiter {
    // Exemplo: 20 requisições por segundo por IP/Chave
    private final Bucket bucket = Bucket.builder()
            .addLimit(Bandwidth.classic(20, Refill.intervally(20, Duration.ofSeconds(1))))
            .build();

    public void acquireToken() {
        if (!bucket.tryConsume(1)) {
            // Aguarda ou bloqueia com timeout para não violar quota
            bucket.asBlocking().consumeUninterruptibly(1);
        }
    }
}
```

#### 5.2 Despacho de Ordens REST com HMAC-SHA256 e Idempotência
1. Gerar `clientOrderId` (ex: `ORD-UUID`) exclusivo por evento.
2. Calcular assinatura HMAC-SHA256 da query string + body usando a `secretKey` descriptografada da conta.
3. Se a conexão cair, a retransmissão com o mesmo `clientOrderId` é idempotente na MEXC.

#### 5.3 WebSocket Privado da MEXC (User Data Stream)
Conectar ao endpoint privado da MEXC (`wss://wbs-api.mexc.com/ws`) com login assinado para escutar confirmações reais de execução (`FILLED`, `CANCELED`, `REJECTED`) e atualizar o saldo local.

#### 5.4 Mecanismo de Emergência: Kill Switch Global
Disponibilizar endpoint de alta prioridade:
```http
POST /v1/operacoes/kill-switch
```
* **Ação imediata:** Cancela todas as ordens abertas na MEXC de todos os robôs, remove subscrições do WebSocket público e encerra operações ativas.

---

### FASE 6: BFF Embutido e Integração Frontend

1. **Broker WebSocket/SSE no Spring Boot:**
   * Utilizar `@EnableWebSocketMessageBroker` com STOMP ou `SseEmitter` direto para push de atualizações para a interface:
     * Cotações e viradas de candle (para atualizar os gráficos em tempo real).
     * Notificações de execução de ordens e alertas de margem.
2. **Rate Limiting no Gateway:**
   * Utilizar o Redis existente para proteger endpoints de login e simulações pesadas contra sobrecarga do frontend.

---

### FASE 7: Validação e Testes de Integridade da Arquitetura

Criar teste unitário utilizando Spring Modulith para assegurar que nenhum desenvolvedor viole as fronteiras de pacotes:

```java
package br.com.bot_mexc;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ArchitectureIntegrityTest {

    @Test
    void verificarIntegridadeDosModulos() {
        ApplicationModules modules = ApplicationModules.of(Application.class);
        modules.verify(); // Falha a compilação se houver acoplamento cíclico ou indevido
    }
}
```

---

## 4. Checklist de Execução

- [x] **Docker Compose:** Atualizar para imagem `timescale/timescaledb:latest-pg16`.
- [x] **Build:** Adicionar dependências (`spring-modulith`, `bucket4j`, `caffeine`).
- [x] **Pacotes:** Criar diretórios de módulos e mover classes conforme mapeamento da Seção 2.
- [x] **Eventos:** Criar `TradeSignalEvent` e substituir RabbitMQ por `ApplicationEventPublisher` no fluxo crítico de ordens.
- [x] **TimescaleDB:** Criar migração Flyway para Hypertable e compressão da tabela `candles`.
- [x] **JDBC Batch:** Implementar `CandleJdbcBatchService` com `JdbcTemplate.batchUpdate`.
- [x] **OMS:** Implementar `MexcRateLimiter` com Bucket4j.
- [x] **OMS:** Implementar assinatura HMAC-SHA256 e `clientOrderId` para chamadas REST na MEXC.
- [x] **OMS:** Implementar listener do WebSocket Privado da MEXC (`spot@private.orders.v3.api`).
- [x] **Segurança:** Implementar `Kill Switch` global de emergência.
- [x] **BFF:** Configurar STOMP/SSE para streaming de eventos de trade para o Frontend.
- [x] **Qualidade:** Validar integridade com `ApplicationModules.verify()`.

---

## 5. Estratégia Futura de Desacoplamento (Escalabilidade Horizontal)

Caso no futuro a plataforma atinja milhares de robôs concorrentes exigindo isolamento de hardware:

1. **O módulo `modules.market` (cálculo pesado de indicadores)** é o primeiro candidato natural para ser extraído em um serviço isolado.
2. Por ter sido construído com eventos desacoplados (`TradeSignalEvent`), basta trocar o listener in-memory de volta para uma fila do RabbitMQ.
3. Todo o código de regras de negócio, lógica matemática e integração com a exchange permanece **100% inalterado**, garantindo transição sem atrito.
