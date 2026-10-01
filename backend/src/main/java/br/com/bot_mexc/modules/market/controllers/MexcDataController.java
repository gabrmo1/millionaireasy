package br.com.bot_mexc.modules.market.controllers;
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


import br.com.bot_mexc.modules.timeseries.dtos.CandleDTO;
import br.com.bot_mexc.modules.market.dtos.mexc.SymbolInfoDTO;
import br.com.bot_mexc.modules.market.services.MexcConnectionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/v1/mexc-data")
public class MexcDataController {

    private final MexcConnectionService mexcConnectionService;

    public MexcDataController(MexcConnectionService mexcConnectionService) {
        this.mexcConnectionService = mexcConnectionService;
    }

    @GetMapping("/stablecoin-pairs")
    public ResponseEntity<Page<SymbolInfoDTO>> getStablecoinPairs(@RequestParam(required = false) String quoteAsset,
                                                                  @RequestParam(required = false) String searchTerm,
                                                                  Pageable pageable) {
        return ResponseEntity.ok(mexcConnectionService.getStablecoinPairsPaginated(quoteAsset, searchTerm, pageable));
    }

    @GetMapping("/stablecoins")
    public ResponseEntity<Set<String>> getStablecoins() {
        return ResponseEntity.ok(mexcConnectionService.getStablecoins());
    }

    @GetMapping("/candles")
    public ResponseEntity<List<CandleDTO>> getCandles(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(defaultValue = "1000") String limit
    ) {
        // limit 2000 é o máximo da MEXC, garantimos que não exceda
        int limitInt = Integer.parseInt(limit);
        if (limitInt > 2000) limit = "2000";

        return ResponseEntity.ok(mexcConnectionService.consultarCandles(symbol, interval, limit));
    }
}