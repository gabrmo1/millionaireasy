package br.com.bot_mexc.modules.strategy.controllers;
import br.com.bot_mexc.shared.enums.*;
import br.com.bot_mexc.shared.utils.DateUtils;
import br.com.bot_mexc.shared.entities.BaseEntity;
import br.com.bot_mexc.shared.configs.RabbitMQConfig;
import br.com.bot_mexc.modules.strategy.entities.*;
import br.com.bot_mexc.modules.strategy.dtos.*;
import br.com.bot_mexc.modules.strategy.repositories.*;
import br.com.bot_mexc.modules.strategy.services.*;
import br.com.bot_mexc.modules.strategy.utils.*;
import br.com.bot_mexc.modules.strategy.builders.*;
import br.com.bot_mexc.modules.strategy.services.OperacaoCacheService;
import br.com.bot_mexc.modules.strategy.services.IndicadorStateService;
import br.com.bot_mexc.modules.market.services.MexcConnectionService;
import br.com.bot_mexc.modules.market.services.mexc.MexcSubscriptionService;
import br.com.bot_mexc.modules.strategy.services.AvaliacaoCondicaoService;
import br.com.bot_mexc.modules.strategy.dtos.OperacaoCacheDTO;
import br.com.bot_mexc.modules.timeseries.repositories.AnaliseRepository;
import br.com.bot_mexc.modules.timeseries.services.BacktestCandleProviderService;
import br.com.bot_mexc.modules.timeseries.dtos.CandleDTO;
import br.com.bot_mexc.modules.timeseries.builders.AnaliseBuilder;

import br.com.bot_mexc.modules.strategy.dtos.CriarEstrategiaDTO;
import br.com.bot_mexc.modules.strategy.dtos.EstrategiaDTO;
import br.com.bot_mexc.modules.strategy.services.EstrategiaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/estrategias")
public class EstrategiaController {

    private final EstrategiaService estrategiaService;

    public EstrategiaController(EstrategiaService estrategiaService) {
        this.estrategiaService = estrategiaService;
    }

    @GetMapping
    public ResponseEntity<List<EstrategiaDTO>> findAll() {
        return ResponseEntity.ok(estrategiaService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EstrategiaDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(estrategiaService.findById(id));
    }

    @PostMapping
    public ResponseEntity<Void> criarEstrategia(@RequestBody @Valid CriarEstrategiaDTO request) {
        estrategiaService.criarEstrategia(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateEstrategia(@PathVariable String id, @RequestBody @Valid CriarEstrategiaDTO request) {
        estrategiaService.updateEstrategia(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deletarEstrategia(@PathVariable String id) {
        estrategiaService.deleteEstrategia(id);
        return ResponseEntity.noContent().build();
    }
}