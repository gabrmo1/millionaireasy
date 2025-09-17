package br.com.bot_mexc.controllers;

import br.com.bot_mexc.models.dtos.EstrategiaDTO;
import br.com.bot_mexc.services.EstrategiaService;
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
    public ResponseEntity<Void> criarEstrategia(@RequestBody @Valid EstrategiaDTO request) {
        estrategiaService.criarEstrategia(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateEstrategia(@PathVariable String id, @RequestBody @Valid EstrategiaDTO request) {
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