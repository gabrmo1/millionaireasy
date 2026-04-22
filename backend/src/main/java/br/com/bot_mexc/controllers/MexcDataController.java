package br.com.bot_mexc.controllers;

import br.com.bot_mexc.models.dtos.CandleDTO;
import br.com.bot_mexc.models.dtos.mexc.SymbolInfoDTO;
import br.com.bot_mexc.services.MexcConnectionService;
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