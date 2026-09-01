-- Isolamento lógico no banco de dados. Default garante compatibilidade retroativa (Zero-Downtime).
ALTER TABLE operacoes ADD COLUMN tipo_operacao VARCHAR(20) DEFAULT 'LIVE' NOT NULL;