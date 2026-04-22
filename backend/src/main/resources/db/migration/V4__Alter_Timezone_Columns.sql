DO $$
    DECLARE
        r RECORD;
    BEGIN
        FOR r IN
            SELECT table_name, column_name
            FROM information_schema.columns
            WHERE table_schema = current_schema()
              AND data_type = 'timestamp without time zone'
              AND table_name IN (
                                 'operadores', 'estrategias', 'indicadores_config',
                                 'condicoes_compra', 'condicoes_venda', 'operacoes',
                                 'pausas_operacoes', 'candles', 'analises',
                                 'compras', 'vendas'
                )
            LOOP
            -- Executa a alteração dinâmica.
            -- O uso de "AT TIME ZONE 'America/Sao_Paulo'" garante que os dados já gravados
            -- com o fuso brasileiro antigo sejam convertidos para o ponto exato no tempo em UTC.
                EXECUTE format(
                        'ALTER TABLE %I ALTER COLUMN %I TYPE TIMESTAMP WITH TIME ZONE USING %I AT TIME ZONE ''America/Sao_Paulo''',
                        r.table_name, r.column_name, r.column_name
                        );
            END LOOP;
    END $$;