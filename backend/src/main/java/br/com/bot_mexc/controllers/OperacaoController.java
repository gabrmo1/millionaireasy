package br.com.bot_mexc.controllers;

import br.com.bot_mexc.models.dtos.CriarOperacaoDTO;
import br.com.bot_mexc.models.dtos.OperacaoDTO;
import br.com.bot_mexc.services.OperacoesService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/operacoes")
public class OperacaoController {

    private final OperacoesService operacoesService;

    public OperacaoController(OperacoesService operacoesService) {
        this.operacoesService = operacoesService;
    }

    @GetMapping
    public ResponseEntity<List<OperacaoDTO>> findAll() {
        return ResponseEntity.ok(operacoesService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OperacaoDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(operacoesService.findById(id));
    }

    @PostMapping("/criar")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Void> criarOperacao(@RequestBody @Valid CriarOperacaoDTO request) {
        operacoesService.criarOperacao(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateOperacao(@PathVariable String id, @RequestBody @Valid CriarOperacaoDTO request) {
        operacoesService.updateOperacao(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deletarOperacao(@PathVariable String id) {
        operacoesService.deletarOperacao(id);
        return ResponseEntity.noContent().build();
    }
}