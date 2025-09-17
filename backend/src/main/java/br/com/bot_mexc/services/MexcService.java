package br.com.bot_mexc.services;

import br.com.bot_mexc.integrations.MexcIntegration;
import br.com.bot_mexc.models.dtos.CandleDTO;
import br.com.bot_mexc.utils.CandleUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class MexcService {

    private final MexcIntegration integration;

    public MexcService(MexcIntegration integration) {
        this.integration = integration;
    }

    public List<CandleDTO> consultarCandles(String symbol, String interval, String limit) {
        return CandleUtils.montarCandles(integration.obterCandles(symbol, interval, limit));
    }

    public BigDecimal consultarValorAtualMoeda(String symbol) {
        return integration.obterValorAtualMoeda(symbol).price();
    }

}
