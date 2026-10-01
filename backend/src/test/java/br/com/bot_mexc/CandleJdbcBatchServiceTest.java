package br.com.bot_mexc;

import br.com.bot_mexc.modules.timeseries.dtos.CandleDTO;
import br.com.bot_mexc.modules.timeseries.services.CandleJdbcBatchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandleJdbcBatchServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private CandleJdbcBatchService candleJdbcBatchService;

    @Test
    @DisplayName("Deve executar batchUpdate no JdbcTemplate com lista de candles")
    void deveExecutarBatchUpdateComCandles() {
        List<CandleDTO> candles = List.of(
                new CandleDTO(1700000000L, 1700000900L, new BigDecimal("60000"), new BigDecimal("60500"), new BigDecimal("59900"), new BigDecimal("60600"), new BigDecimal("12.5")),
                new CandleDTO(1700000900L, 1700001800L, new BigDecimal("60500"), new BigDecimal("61000"), new BigDecimal("60400"), new BigDecimal("61100"), new BigDecimal("18.2"))
        );

        when(jdbcTemplate.batchUpdate(anyString(), any(BatchPreparedStatementSetter.class))).thenReturn(new int[]{1, 1});

        candleJdbcBatchService.batchInsertCandles(candles, "BTCUSDT", "15m");

        verify(jdbcTemplate, times(1)).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
    }

    @Test
    @DisplayName("Não deve chamar JdbcTemplate se a lista for vazia")
    void naoDeveChamarComListaVazia() {
        candleJdbcBatchService.batchInsertCandles(List.of(), "BTCUSDT", "15m");
        verify(jdbcTemplate, never()).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
    }
}
