package br.com.bot_mexc.controllers;

import br.com.bot_mexc.models.dtos.CriarOperacaoDTO;
import br.com.bot_mexc.models.dtos.HistoricoOperacaoDTO;
import br.com.bot_mexc.models.dtos.OperacaoDTO;
import br.com.bot_mexc.models.dtos.monitoramento.MonitoramentoDataDTO;
import br.com.bot_mexc.models.dtos.monitoramento.RelatorioDesempenhoDTO;
import br.com.bot_mexc.services.MonitoramentoService;
import br.com.bot_mexc.services.OperacoesService;
import br.com.bot_mexc.services.RelatorioDesempenhoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/operacoes")
public class OperacaoController {

    private final MonitoramentoService monitoramentoService;
    private final OperacoesService operacoesService;
    private final RelatorioDesempenhoService relatorioDesempenhoService;

    public OperacaoController(OperacoesService operacoesService,
                              MonitoramentoService monitoramentoService,
                              RelatorioDesempenhoService relatorioDesempenhoService) {
        this.monitoramentoService = monitoramentoService;
        this.operacoesService = operacoesService;
        this.relatorioDesempenhoService = relatorioDesempenhoService;
    }

    @GetMapping
    public ResponseEntity<List<OperacaoDTO>> findAll() {
        return ResponseEntity.ok(operacoesService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OperacaoDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(operacoesService.findById(id));
    }

    @GetMapping("/{id}/historico")
    public ResponseEntity<HistoricoOperacaoDTO> buscarHistorico(@PathVariable String id) {
        return ResponseEntity.ok(operacoesService.buscarHistorico(id));
    }

    @PostMapping
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

    @PostMapping("/{id}/start")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Void> iniciarOperacao(@PathVariable String id) {
        operacoesService.iniciarOperacao(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/stop")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Void> pararOperacao(@PathVariable String id) {
        operacoesService.pararOperacao(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/monitoramento")
    public ResponseEntity<MonitoramentoDataDTO> buscarDadosMonitoramento(@PathVariable String id) {
        return ResponseEntity.ok(monitoramentoService.buscarDadosMonitoramento(id));
    }

    /**
     * Endpoint para consumo do Relatório de Desempenho.
     * Os atributos numéricos problemáticos (divisões por zero, arrays vazios) já foram limpos e
     * estabilizados na Engine do RelatorioDesempenhoService, retornando 'null' ou '0' adequadamente
     * para consumo direto do JSON pelo Frontend.
     */
    @GetMapping("/{id}/relatorio-desempenho")
    public ResponseEntity<RelatorioDesempenhoDTO> gerarRelatorioDesempenho(@PathVariable String id) {
        return ResponseEntity.ok(relatorioDesempenhoService.gerarRelatorio(id));
    }

}