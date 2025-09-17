package br.com.bot_mexc.controllers;

import br.com.bot_mexc.models.dtos.CriarOperadorDTO;
import br.com.bot_mexc.models.dtos.OperadorDTO;
import br.com.bot_mexc.services.OperadorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/operadores")
public class OperadorController {

    private final OperadorService operadorService;

    public OperadorController(OperadorService operadorService) {
        this.operadorService = operadorService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<OperadorDTO>> findAll() {
        return ResponseEntity.ok(operadorService.findAll());
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<OperadorDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(operadorService.findById(id));
    }

    @PostMapping("/criar")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Void> criarOperador(@RequestBody @Valid CriarOperadorDTO request) {
        operadorService.criarOperador(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Void> updateOperador(@PathVariable String id, @RequestBody @Valid CriarOperadorDTO request) {
        operadorService.updateOperador(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteOperador(@PathVariable String id) {
        operadorService.deleteOperador(id);
        return ResponseEntity.noContent().build();
    }

}