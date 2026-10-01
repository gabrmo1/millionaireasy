-- ==============================================================================
-- Migration V6: Configuração de Hypertable TimescaleDB e Compressão de Candles
-- ==============================================================================

-- 1. Habilitar extensão TimescaleDB
CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;

-- 2. No TimescaleDB, a chave primária de uma Hypertable DEVE conter a coluna de tempo.
-- Ajustamos a constraint pk_candles para chave composta (id, data_fechamento).
ALTER TABLE candles DROP CONSTRAINT IF EXISTS pk_candles;
ALTER TABLE candles ADD CONSTRAINT pk_candles PRIMARY KEY (id, data_fechamento);

-- 3. Índice único para upsert / prevenção de duplicatas
CREATE UNIQUE INDEX IF NOT EXISTS uq_candles_par_intervalo_fechamento 
    ON candles (par, intervalo, data_fechamento);

-- 4. Converter a tabela candles em Hypertable particionada por data_fechamento
SELECT create_hypertable('candles', 'data_fechamento', if_not_exists => TRUE, migrate_data => TRUE);

-- 5. Ativar compressão nativa do TimescaleDB (economiza ~90% de I/O e armazenamento em disco)
ALTER TABLE candles SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'par, intervalo',
    timescaledb.compress_orderby = 'data_fechamento DESC'
);

-- 6. Política de compressão automática para dados históricos com mais de 7 dias
SELECT add_compression_policy('candles', INTERVAL '7 days', if_not_exists => TRUE);
