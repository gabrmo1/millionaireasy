-- 1. Tabela de Usuários (Auth)
CREATE TABLE IF NOT EXISTS usuarios
(
    id       VARCHAR(255) NOT NULL,
    email    VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(255),
    CONSTRAINT pk_usuarios PRIMARY KEY (id),
    CONSTRAINT uc_usuarios_email UNIQUE (email)
);

-- 2. Tabela de Operadores (Credenciais API)
CREATE TABLE IF NOT EXISTS operadores
(
    id           VARCHAR(36)  NOT NULL,
    data_criacao TIMESTAMP(6),
    id_usuario   VARCHAR(36)  NOT NULL,
    nome         VARCHAR(50)  NOT NULL,
    access_key   VARCHAR(255) NOT NULL,
    secret_key   VARCHAR(255) NOT NULL,
    CONSTRAINT pk_operadores PRIMARY KEY (id)
);

-- 3. Tabela de Estratégias
CREATE TABLE IF NOT EXISTS estrategias
(
    id                        VARCHAR(36) NOT NULL,
    data_criacao              TIMESTAMP(6),
    id_usuario                VARCHAR(36) NOT NULL,
    nome                      VARCHAR(50) NOT NULL,
    valor_operacao_fixo       NUMERIC(38, 2),
    stablecoin                VARCHAR(10),
    percentual_valor_operacao NUMERIC(38, 2),
    venda_apenas_por_lucro    BOOLEAN,
    percentual_lucro          NUMERIC(38, 2),
    CONSTRAINT pk_estrategias PRIMARY KEY (id)
);

-- 4. Configuração de Indicadores (JSONB)
CREATE TABLE IF NOT EXISTS indicadores_config
(
    id             VARCHAR(36)  NOT NULL,
    data_criacao   TIMESTAMP(6),
    id_usuario     VARCHAR(36)  NOT NULL,
    id_estrategia  VARCHAR(36)  NOT NULL,
    alias          VARCHAR(255) NOT NULL,
    tipo_indicador VARCHAR(255) NOT NULL,
    parametros     JSONB,
    CONSTRAINT pk_indicadores_config PRIMARY KEY (id),
    CONSTRAINT fk_indicadores_estrategia FOREIGN KEY (id_estrategia) REFERENCES estrategias (id)
);

-- 5. Condições de Compra
CREATE TABLE IF NOT EXISTS condicoes_compra
(
    id                    VARCHAR(36)  NOT NULL,
    data_criacao          TIMESTAMP(6),
    id_usuario            VARCHAR(36)  NOT NULL,
    id_estrategia         VARCHAR(36)  NOT NULL,
    ordem                 INTEGER      NOT NULL,
    operador_para_proxima VARCHAR(255),
    operando_a_tipo       VARCHAR(255) NOT NULL,
    operando_a_referencia VARCHAR(255),
    operando_a_valor      NUMERIC(38, 2),
    operador              VARCHAR(255) NOT NULL,
    operando_b_tipo       VARCHAR(255) NOT NULL,
    operando_b_referencia VARCHAR(255),
    operando_b_valor      NUMERIC(38, 2),
    CONSTRAINT pk_condicoes_compra PRIMARY KEY (id),
    CONSTRAINT fk_condicoes_compra_estrategia FOREIGN KEY (id_estrategia) REFERENCES estrategias (id)
);

-- 6. Condições de Venda
CREATE TABLE IF NOT EXISTS condicoes_venda
(
    id                    VARCHAR(36)  NOT NULL,
    data_criacao          TIMESTAMP(6),
    id_usuario            VARCHAR(36)  NOT NULL,
    id_estrategia         VARCHAR(36)  NOT NULL,
    ordem                 INTEGER      NOT NULL,
    operador_para_proxima VARCHAR(255),
    operando_a_tipo       VARCHAR(255) NOT NULL,
    operando_a_referencia VARCHAR(255),
    operando_a_valor      NUMERIC(38, 2),
    operador              VARCHAR(255) NOT NULL,
    operando_b_tipo       VARCHAR(255) NOT NULL,
    operando_b_referencia VARCHAR(255),
    operando_b_valor      NUMERIC(38, 2),
    CONSTRAINT pk_condicoes_venda PRIMARY KEY (id),
    CONSTRAINT fk_condicoes_venda_estrategia FOREIGN KEY (id_estrategia) REFERENCES estrategias (id)
);

-- 7. Operações (Execução do Bot)
CREATE TABLE IF NOT EXISTS operacoes
(
    id            VARCHAR(36) NOT NULL,
    data_criacao  TIMESTAMP(6),
    id_usuario    VARCHAR(36) NOT NULL,
    status        VARCHAR(12) NOT NULL,
    data_inicio   TIMESTAMP(6),
    data_fim      TIMESTAMP(6),
    par           VARCHAR(20) NOT NULL,
    intervalo     VARCHAR(5)  NOT NULL,
    modo_teste    BOOLEAN     NOT NULL,
    saldo_inicial NUMERIC(38, 2),
    id_operador   VARCHAR(36),
    id_estrategia VARCHAR(36),
    CONSTRAINT pk_operacoes PRIMARY KEY (id),
    CONSTRAINT fk_operacoes_operador FOREIGN KEY (id_operador) REFERENCES operadores (id),
    CONSTRAINT fk_operacoes_estrategia FOREIGN KEY (id_estrategia) REFERENCES estrategias (id)
);

-- 8. Pausas de Operação
CREATE TABLE IF NOT EXISTS pausas_operacoes
(
    id           VARCHAR(36) NOT NULL,
    data_criacao TIMESTAMP(6),
    id_usuario   VARCHAR(36) NOT NULL,
    id_operacao  VARCHAR(36) NOT NULL,
    data_pausa   TIMESTAMP(6),
    CONSTRAINT pk_pausas_operacoes PRIMARY KEY (id),
    CONSTRAINT fk_pausas_operacao FOREIGN KEY (id_operacao) REFERENCES operacoes (id)
);

-- 9. Candles (Histórico de Preço)
CREATE TABLE IF NOT EXISTS candles
(
    id               VARCHAR(36)    NOT NULL,
    data_criacao     TIMESTAMP(6),
    id_usuario       VARCHAR(36)    NOT NULL,
    par              VARCHAR(20)    NOT NULL,
    intervalo        VARCHAR(255)   NOT NULL,
    data_abertura    TIMESTAMP(6)   NOT NULL,
    data_fechamento  TIMESTAMP(6)   NOT NULL,
    valor_abertura   NUMERIC(38, 2) NOT NULL,
    valor_fechamento NUMERIC(38, 2) NOT NULL,
    maxima           NUMERIC(38, 2) NOT NULL,
    minima           NUMERIC(38, 2) NOT NULL,
    volume           NUMERIC(38, 2) NOT NULL,
    CONSTRAINT pk_candles PRIMARY KEY (id)
);

-- 10. Análises (Snapshot Técnico)
CREATE TABLE IF NOT EXISTS analises
(
    id                           VARCHAR(36)    NOT NULL,
    data_criacao                 TIMESTAMP(6),
    id_usuario                   VARCHAR(36)    NOT NULL,
    par                          VARCHAR(20)    NOT NULL,
    intervalo                    VARCHAR(255)   NOT NULL,
    valor_atual_moeda            NUMERIC(38, 2) NOT NULL,
    data_analise                 TIMESTAMP(6)   NOT NULL,

    -- Parâmetros usados
    periodo_ema                  INTEGER,
    periodo_sma                  INTEGER,
    periodo_rsi_curto            INTEGER        NOT NULL,
    periodo_rsi_medio            INTEGER        NOT NULL,
    periodo_rsi_longo            INTEGER        NOT NULL,
    periodo_rsi_estocastico      INTEGER        NOT NULL,
    suavizacao_rsi_estocastico_d INTEGER        NOT NULL,
    suavizacao_rsi_estocastico_k INTEGER        NOT NULL,

    -- Valores calculados
    ema                          NUMERIC(38, 2) NOT NULL,
    sma                          NUMERIC(38, 2) NOT NULL,
    rsi_curto                    NUMERIC(38, 2) NOT NULL,
    rsi_medio                    NUMERIC(38, 2) NOT NULL,
    rsi_longo                    NUMERIC(38, 2) NOT NULL,
    rsi_estocastico_d            NUMERIC(38, 2) NOT NULL,
    rsi_estocastico_k            NUMERIC(38, 2) NOT NULL,
    volume                       NUMERIC(38, 2) NOT NULL,

    CONSTRAINT pk_analises PRIMARY KEY (id)
);

-- 11. Compras (Executadas)
CREATE TABLE IF NOT EXISTS compras
(
    id                   VARCHAR(36)    NOT NULL,
    data_criacao         TIMESTAMP(6),
    id_usuario           VARCHAR(36)    NOT NULL,
    id_operacao          VARCHAR(36)    NOT NULL,
    data_compra          TIMESTAMP(6)   NOT NULL,
    valor_operacao       NUMERIC(38, 2) NOT NULL,
    valor_moeda          NUMERIC(38, 2) NOT NULL,
    volume               NUMERIC(38, 2) NOT NULL,
    snapshot_indicadores TEXT,
    CONSTRAINT pk_compras PRIMARY KEY (id),
    CONSTRAINT fk_compras_operacao FOREIGN KEY (id_operacao) REFERENCES operacoes (id)
);

-- 12. Vendas (Executadas)
CREATE TABLE IF NOT EXISTS vendas
(
    id                   VARCHAR(36)    NOT NULL,
    data_criacao         TIMESTAMP(6),
    id_usuario           VARCHAR(36)    NOT NULL,
    id_operacao          VARCHAR(36)    NOT NULL,
    data_venda           TIMESTAMP(6)   NOT NULL,
    valor_compra         NUMERIC(38, 2) NOT NULL,
    valor_venda          NUMERIC(38, 2) NOT NULL,
    lucro                NUMERIC(38, 2) NOT NULL,
    snapshot_indicadores TEXT,
    CONSTRAINT pk_vendas PRIMARY KEY (id),
    CONSTRAINT fk_vendas_operacao FOREIGN KEY (id_operacao) REFERENCES operacoes (id)
);