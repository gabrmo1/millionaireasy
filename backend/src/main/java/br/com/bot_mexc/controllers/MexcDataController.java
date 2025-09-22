package br.com.bot_mexc.controllers;

import br.com.bot_mexc.models.dtos.mexc.SymbolInfoDTO;
import br.com.bot_mexc.services.MexcService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/mexc-data")
public class MexcDataController {

    private final MexcService mexcService;

    public MexcDataController(MexcService mexcService) {
        this.mexcService = mexcService;
    }

    @GetMapping("/stablecoin-pairs")
    public ResponseEntity<List<SymbolInfoDTO>> getStablecoinPairs() {
        return ResponseEntity.ok(mexcService.getStablecoinPairs());
    }
}