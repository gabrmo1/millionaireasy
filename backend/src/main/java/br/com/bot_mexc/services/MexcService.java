package br.com.bot_mexc.services;

import br.com.bot_mexc.integrations.MexcIntegration;
import br.com.bot_mexc.models.dtos.CandleDTO;
import br.com.bot_mexc.models.dtos.mexc.SymbolInfoDTO;
import br.com.bot_mexc.utils.CandleUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MexcService {

    private final MexcIntegration integration;
    private static final Set<String> STABLECOINS = Set.of("USDT", "USDC", "EUR");

    public MexcService(MexcIntegration integration) {
        this.integration = integration;
    }

    public List<CandleDTO> consultarCandles(String symbol, String interval, String limit) {
        return CandleUtils.montarCandles(integration.obterCandles(symbol, interval, limit));
    }

    public BigDecimal consultarValorAtualMoeda(String symbol) {
        return integration.obterValorAtualMoeda(symbol).price();
    }

    public List<SymbolInfoDTO> getStablecoinPairs() {
        return integration.getExchangeInfo().getSymbols().stream()
                .filter(symbol -> "1".equals(symbol.getStatus()) && STABLECOINS.contains(symbol.getQuoteAsset()))
                .collect(Collectors.toList());
    }
}