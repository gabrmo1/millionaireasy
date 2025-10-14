package br.com.bot_mexc.controllers;

import br.com.bot_mexc.models.dtos.mexc.SymbolInfoDTO;
import br.com.bot_mexc.services.MexcService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/v1/mexc-data")
public class MexcDataController {

    private final MexcService mexcService;

    public MexcDataController(MexcService mexcService) {
        this.mexcService = mexcService;
    }

    @GetMapping("/stablecoin-pairs")
    public ResponseEntity<Page<SymbolInfoDTO>> getStablecoinPairs(@RequestParam(required = false) String quoteAsset,
                                                                  @RequestParam(required = false) String searchTerm,
                                                                  Pageable pageable) {
        return ResponseEntity.ok(mexcService.getStablecoinPairsPaginated(quoteAsset, searchTerm, pageable));
    }

    @GetMapping("/stablecoins")
    public ResponseEntity<Set<String>> getStablecoins() {
        return ResponseEntity.ok(mexcService.getStablecoins());
    }
}