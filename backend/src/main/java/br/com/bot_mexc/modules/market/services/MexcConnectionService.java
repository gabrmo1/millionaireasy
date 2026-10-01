package br.com.bot_mexc.modules.market.services;
import br.com.bot_mexc.shared.enums.*;
import br.com.bot_mexc.shared.utils.DateUtils;
import br.com.bot_mexc.shared.constants.IndicadorKeys;
import br.com.bot_mexc.shared.configs.RabbitMQConfig;
import br.com.bot_mexc.shared.configs.RedisConfig;
import br.com.bot_mexc.modules.market.services.*;
import br.com.bot_mexc.modules.market.services.mexc.*;
import br.com.bot_mexc.modules.strategy.services.indicators.*;
import br.com.bot_mexc.modules.market.dtos.*;
import br.com.bot_mexc.modules.market.dtos.mexc.*;
import br.com.bot_mexc.modules.strategy.dtos.monitoramento.*;
import br.com.bot_mexc.modules.strategy.entities.CondicaoCompra;
import br.com.bot_mexc.modules.strategy.entities.CondicaoVenda;
import br.com.bot_mexc.modules.strategy.entities.IndicadorConfig;
import br.com.bot_mexc.modules.strategy.entities.Operacao;
import br.com.bot_mexc.modules.strategy.repositories.OperacaoRepository;
import br.com.bot_mexc.modules.strategy.repositories.CompraRepository;
import br.com.bot_mexc.modules.strategy.repositories.VendaRepository;
import br.com.bot_mexc.modules.timeseries.dtos.CandleDTO;
import br.com.bot_mexc.modules.timeseries.services.CandleService;
import br.com.bot_mexc.modules.timeseries.services.AnaliseService;
import br.com.bot_mexc.modules.timeseries.repositories.AnaliseRepository;
import br.com.bot_mexc.modules.timeseries.builders.AnaliseBuilder;


import br.com.bot_mexc.modules.market.integrations.MexcIntegration;
import br.com.bot_mexc.modules.timeseries.dtos.CandleDTO;
import br.com.bot_mexc.modules.market.dtos.mexc.SymbolInfoDTO;
import br.com.bot_mexc.modules.timeseries.utils.CandleUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MexcConnectionService {

    private final MexcIntegration integration;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final Set<String> STABLECOINS = Set.of("USDT", "USDC", "EUR");
    private static final String STABLECOIN_PAIRS_CACHE_KEY = "stablecoinPairs";

    public MexcConnectionService(MexcIntegration integration, RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.integration = integration;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public List<CandleDTO> consultarCandles(String symbol, String interval, String limit) {
        return CandleUtils.buildListCandleDtoFromMexcResponse(integration.obterCandles(symbol, interval, limit, null, null));
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

    private List<SymbolInfoDTO> getStablecoinPairsFromCache() {
        Object cachedData = redisTemplate.opsForValue().get(STABLECOIN_PAIRS_CACHE_KEY);
        if (cachedData != null) {
            return ((List<?>) cachedData).stream()
                    .map(item -> objectMapper.convertValue(item, SymbolInfoDTO.class))
                    .collect(Collectors.toList());
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