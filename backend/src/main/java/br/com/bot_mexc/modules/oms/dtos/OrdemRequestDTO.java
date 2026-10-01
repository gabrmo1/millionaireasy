package br.com.bot_mexc.modules.oms.dtos;
import br.com.bot_mexc.shared.enums.*;
import br.com.bot_mexc.shared.configs.RabbitMQConfig;
import br.com.bot_mexc.modules.oms.services.*;
import br.com.bot_mexc.modules.oms.dtos.*;
import br.com.bot_mexc.modules.oms.integrations.*;
import br.com.bot_mexc.modules.strategy.services.OperacaoCacheService;
import br.com.bot_mexc.modules.strategy.dtos.OperacaoCacheDTO;
import br.com.bot_mexc.modules.strategy.services.SimulacaoService;

import java.math.BigDecimal;
import java.util.Map;

public record OrdemRequestDTO(
        String idOperacao,
        String par,
        String intervalo,
        BigDecimal preco,
        TipoOrdem tipo,
        Map<String, BigDecimal> indicadores,
        long dataCandle
) {
    public enum TipoOrdem {
        BUY, SELL
    }
}