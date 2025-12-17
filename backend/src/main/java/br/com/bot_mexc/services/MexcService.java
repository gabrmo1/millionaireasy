package br.com.bot_mexc.services;

import br.com.bot_mexc.integrations.MexcIntegration;
import br.com.bot_mexc.models.dtos.CandleDTO;
import br.com.bot_mexc.models.dtos.mexc.SymbolInfoDTO;
import br.com.bot_mexc.utils.CandleUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MexcService {

    private final MexcIntegration integration;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final Set<String> STABLECOINS = Set.of("USDT", "USDC", "EUR");
    private static final String STABLECOIN_PAIRS_CACHE_KEY = "stablecoinPairs";

    public MexcService(MexcIntegration integration, RedisTemplate<String, Object> redisTemplate) {
        this.integration = integration;
        this.redisTemplate = redisTemplate;
    }

    public List<CandleDTO> consultarCandles(String symbol, String interval, String limit) {
        return CandleUtils.montarCandles(integration.obterCandles(symbol, interval, limit));
    }

    public BigDecimal consultarValorAtualMoeda(String symbol) {
        return integration.obterValorAtualMoeda(symbol).price();
    }

    public Page<SymbolInfoDTO> getStablecoinPairsPaginated(String quoteAsset, String searchTerm, Pageable pageable) {
        List<SymbolInfoDTO> allPairs = getStablecoinPairsFromCache();

        Stream<SymbolInfoDTO> stream = allPairs.stream();

        if (quoteAsset != null && !quoteAsset.isBlank()) {
            stream = stream.filter(p -> quoteAsset.equals(p.getQuoteAsset()));
        }

        if (searchTerm != null && !searchTerm.isBlank()) {
            stream = stream.filter(p -> p.getSymbol().toLowerCase().contains(searchTerm.toLowerCase()));
        }

        List<SymbolInfoDTO> filteredPairs = stream.collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredPairs.size());

        List<SymbolInfoDTO> pageContent = (start >= filteredPairs.size())
                ? Collections.emptyList()
                : filteredPairs.subList(start, end);

        return new PageImpl<>(pageContent, pageable, filteredPairs.size());
    }

    @SuppressWarnings("unchecked")
    private List<SymbolInfoDTO> getStablecoinPairsFromCache() {
        Object cachedData = redisTemplate.opsForValue().get(STABLECOIN_PAIRS_CACHE_KEY);
        if (cachedData != null) {
            return (List<SymbolInfoDTO>) cachedData;
        } else {
            List<SymbolInfoDTO> allPairs = integration.getExchangeInfo().getSymbols().stream()
                    .filter(symbol -> "1".equals(symbol.getStatus()) && STABLECOINS.contains(symbol.getQuoteAsset()))
                    .collect(Collectors.toList());
            redisTemplate.opsForValue().set(STABLECOIN_PAIRS_CACHE_KEY, allPairs, 24, TimeUnit.HOURS);
            return allPairs;
        }
    }

    public Set<String> getStablecoins() {
        return STABLECOINS;
    }
}