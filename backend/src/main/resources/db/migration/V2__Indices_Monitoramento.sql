-- Índices para otimizar o carregamento do Modal de Monitoramento

-- 1. Analises (Busca composta por Par + Intervalo + Data)
CREATE INDEX IF NOT EXISTS idx_analises_busca_monitoramento
    ON analises (par, intervalo, data_analise);

-- 2. Candles (Busca composta por Par + Intervalo + Data de Abertura)
CREATE INDEX IF NOT EXISTS idx_candles_busca
    ON candles (par, intervalo, data_abertura);

-- 3. Compras (Filtro por Operação)
CREATE INDEX IF NOT EXISTS idx_compras_operacao
    ON compras (id_operacao, data_compra);

-- 4. Vendas (Filtro por Operação)
CREATE INDEX IF NOT EXISTS idx_vendas_operacao
    ON vendas (id_operacao, data_venda);

-- 5. Operações (Opcional - Busca por Usuario)
CREATE INDEX IF NOT EXISTS idx_operacoes_usuario
    ON operacoes (id_usuario, status);