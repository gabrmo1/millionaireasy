package br.com.bot_mexc.modules.timeseries.services;

import br.com.bot_mexc.modules.timeseries.dtos.CandleDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandleJdbcBatchService {

    private final JdbcTemplate jdbcTemplate;

    private static final String SQL_BATCH_INSERT = """
        INSERT INTO candles (
            id, data_criacao, id_usuario, par, intervalo, 
            data_abertura, data_fechamento, valor_abertura, 
            valor_fechamento, maxima, minima, volume
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (par, intervalo, data_fechamento) DO UPDATE SET
            valor_abertura = EXCLUDED.valor_abertura,
            valor_fechamento = EXCLUDED.valor_fechamento,
            maxima = EXCLUDED.maxima,
            minima = EXCLUDED.minima,
            volume = EXCLUDED.volume
    """;

    @Transactional
    public void batchInsertCandles(List<CandleDTO> candles, String par, String intervalo) {
        batchInsertCandles(candles, par, intervalo, "SYSTEM");
    }

    @Transactional
    public void batchInsertCandles(List<CandleDTO> candles, String par, String intervalo, String idUsuario) {
        if (candles == null || candles.isEmpty()) {
            return;
        }

        final long inicioMs = System.currentTimeMillis();
        final String userId = (idUsuario != null && !idUsuario.isBlank()) ? idUsuario : "SYSTEM";
        final Timestamp dataCriacao = new Timestamp(System.currentTimeMillis());

        jdbcTemplate.batchUpdate(SQL_BATCH_INSERT, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                CandleDTO c = candles.get(i);
                ps.setString(1, UUID.randomUUID().toString());
                ps.setTimestamp(2, dataCriacao);
                ps.setString(3, userId);
                ps.setString(4, par);
                ps.setString(5, intervalo);
                ps.setTimestamp(6, Timestamp.from(Instant.ofEpochSecond(c.dataAbertura())));
                ps.setTimestamp(7, Timestamp.from(Instant.ofEpochSecond(c.dataFechamento())));
                ps.setBigDecimal(8, c.valorAbertura());
                ps.setBigDecimal(9, c.valorFechamento());
                ps.setBigDecimal(10, c.maxima());
                ps.setBigDecimal(11, c.minima());
                ps.setBigDecimal(12, c.volume());
            }

            @Override
            public int getBatchSize() {
                return candles.size();
            }
        });

        final long duracaoMs = System.currentTimeMillis() - inicioMs;
        log.info("[TIMESERIES BATCH] Inserção de {} candles de {}/{} concluída em {} ms via JdbcTemplate.",
                candles.size(), par, intervalo, duracaoMs);
    }
}
