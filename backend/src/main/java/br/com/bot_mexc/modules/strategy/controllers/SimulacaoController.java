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

import br.com.bot_mexc.modules.strategy.dtos.CriarSimulacaoRequestDTO;
import br.com.bot_mexc.modules.strategy.dtos.SimulacaoDTO;
import br.com.bot_mexc.modules.strategy.dtos.monitoramento.MonitoramentoDataDTO;
import br.com.bot_mexc.modules.strategy.dtos.monitoramento.RelatorioDesempenhoDTO;
import br.com.bot_mexc.shared.enums.TipoOperacao;
import br.com.bot_mexc.modules.strategy.repositories.OperacaoRepository;
import br.com.bot_mexc.modules.strategy.services.MonitoramentoService;
import br.com.bot_mexc.modules.strategy.services.RelatorioDesempenhoService;
import br.com.bot_mexc.modules.strategy.services.SimulacaoService;
import br.com.bot_mexc.modules.strategy.utils.EstrategiaUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/simulacoes")
@RequiredArgsConstructor
public class SimulacaoController {

    private final SimulacaoService simulacaoService;
    private final OperacaoRepository operacaoRepository;
    private final MonitoramentoService monitoramentoService;
    private final RelatorioDesempenhoService relatorioDesempenhoService;

    @GetMapping
    public ResponseEntity<List<SimulacaoDTO>> findAll() {
        // Filtragem em memória das operações Eagerly carregadas para isolamento do tipo BACKTEST
        List<SimulacaoDTO> simulacoes = operacaoRepository.findAllEagerly().stream()
                .filter(op -> op.getTipoOperacao() == TipoOperacao.BACKTEST)
                .map(op -> SimulacaoDTO.builder()
                        .id(op.getId())
                        .par(op.getPar())
                        .intervalo(op.getIntervalo())
                        .status(op.getStatus())
                        .estrategia(EstrategiaUtils.converterEntidadeParaDto(op.getEstrategia()))
                        .saldoInicial(op.getSaldoInicial())
                        .dataCriacao(op.getDataCriacao())
                        .dataInicio(op.getDataInicio())
                        .dataFim(op.getDataFim())
                        .build())
                .toList();

        return ResponseEntity.ok(simulacoes);
    }

    @PostMapping
    public ResponseEntity<Void> criarSimulacao(@RequestBody @Valid CriarSimulacaoRequestDTO request) {
        simulacaoService.iniciarSimulacao(request);
        // Retorna 202 Accepted indicando que o processamento assíncrono (RabbitMQ) foi enfileirado
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{id}/monitoramento")
    public ResponseEntity<MonitoramentoDataDTO> buscarDadosMonitoramento(@PathVariable String id) {
        // Reuso Crítico: O motor original constrói os gráficos lendo os buffers persistidos pelo BacktestEngine
        return ResponseEntity.ok(monitoramentoService.buscarDadosMonitoramento(id));
    }

    @GetMapping("/{id}/relatorio-desempenho")
    public ResponseEntity<RelatorioDesempenhoDTO> gerarRelatorioDesempenho(@PathVariable String id) {
        // Reuso Crítico: O motor de High Water Mark e Drawdown processará os trades simulados nativamente
        return ResponseEntity.ok(relatorioDesempenhoService.gerarRelatorio(id));
    }
}