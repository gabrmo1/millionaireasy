package br.com.bot_mexc.services;

import br.com.bot_mexc.configs.RabbitMQConfig;
import br.com.bot_mexc.models.dtos.CriarSimulacaoRequestDTO;
import br.com.bot_mexc.models.dtos.OperacaoCacheDTO;
import br.com.bot_mexc.models.dtos.OrdemRequestDTO;
import br.com.bot_mexc.models.entities.Compra;
import br.com.bot_mexc.models.entities.Estrategia;
import br.com.bot_mexc.models.entities.Operacao;
import br.com.bot_mexc.models.entities.Venda;
import br.com.bot_mexc.models.enums.StatusOperacoes;
import br.com.bot_mexc.models.enums.TipoOperacao;
import br.com.bot_mexc.repositories.CompraRepository;
import br.com.bot_mexc.repositories.EstrategiaRepository;
import br.com.bot_mexc.repositories.OperacaoRepository;
import br.com.bot_mexc.repositories.VendaRepository;
import br.com.bot_mexc.utils.DateUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimulacaoService {

    private final OperacaoRepository operacaoRepository;
    private final CompraRepository compraRepository;
    private final VendaRepository vendaRepository;
    private final EstrategiaRepository estrategiaRepository;
    private final OperacaoCacheService operacaoCacheService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    private static final BigDecimal TAXA_OPERACAO = new BigDecimal("0.001"); // 0.1%

    @Transactional
    public String iniciarSimulacao(CriarSimulacaoRequestDTO request) {
        Estrategia estrategia = estrategiaRepository.findById(request.idEstrategia())
                .orElseThrow(() -> new ValidationException("Estratégia não encontrada."));

        Operacao operacao = Operacao.builder()
                .par(request.par())
                .intervalo(request.intervalo())
                .estrategia(estrategia)
                .modoTeste(true)
                .saldoInicial(request.saldoInicial())
                .tipoOperacao(TipoOperacao.BACKTEST)
                .status(StatusOperacoes.AGUARDANDO)
                .dataInicio(request.dataInicio())
                .dataFim(request.dataFim())
                .build();

        operacao = operacaoRepository.save(operacao);

        // Fire-and-forget: Emite o ID para a fila de processamento, isolando o chamador
        rabbitTemplate.convertAndSend(RabbitMQConfig.SIMULATIONS_PROCESS_QUEUE, operacao.getId());

        log.info("[BACKTEST] Simulação criada e enfileirada. Operação ID: {}", operacao.getId());
        return operacao.getId();
    }

    public void processarOrdemSimulada(OperacaoCacheDTO operacaoCache, OrdemRequestDTO ordem) {
        final var snapshotIndicadores = serializarIndicadores(ordem.indicadores());
        final var dataCandle = ordem.dataCandle();

        if (ordem.tipo() == OrdemRequestDTO.TipoOrdem.BUY) {
            executarCompraSimulada(operacaoCache, ordem.preco(), snapshotIndicadores, dataCandle);
        } else {
            executarVendaSimulada(operacaoCache, ordem.preco(), snapshotIndicadores, dataCandle);
        }
    }

    private void executarCompraSimulada(OperacaoCacheDTO cache, BigDecimal precoAtual, String snapshotIndicadores, long dataCandle) {
        if (cache.posicionado()) {
            return;
        }

        final var saldoDisponivel = cache.saldo();
        var valorInvestimento = calcularValorInvestimento(cache, saldoDisponivel);

        if (valorInvestimento.compareTo(new BigDecimal("5")) < 0) {
            log.warn("[SIMULAÇÃO] Saldo insuficiente ou valor de operação muito baixo para {}: {}", cache.id(), valorInvestimento);
            return;
        }

        if (saldoDisponivel.compareTo(valorInvestimento) < 0) {
            valorInvestimento = saldoDisponivel;
        }

        final var volumeComprado = valorInvestimento.divide(precoAtual, 8, RoundingMode.DOWN);
        final var volumeLiquido = volumeComprado.subtract(volumeComprado.multiply(TAXA_OPERACAO));
        final var novoSaldo = saldoDisponivel.subtract(valorInvestimento);

        operacaoCacheService.atualizarEstadoAposCompra(cache.id(), cache.par(), cache.intervalo(), novoSaldo, precoAtual, volumeLiquido);

        salvarCompraNoBanco(cache.id(), precoAtual, valorInvestimento, volumeLiquido, novoSaldo, snapshotIndicadores, dataCandle);

        log.info("[SIMULAÇÃO] COMPRA Executada. Vol: {} @ {}. Saldo Restante: {}", volumeLiquido, precoAtual, novoSaldo);
    }

    private void executarVendaSimulada(OperacaoCacheDTO cache, BigDecimal precoAtual, String snapshotIndicadores, long dataCandle) {
        if (!cache.posicionado())
            return;

        final var volumeVendido = cache.volumeEmMao();
        final var precoEntrada = cache.precoMedioEntrada();
        final var custoTotalInvestido = precoEntrada.multiply(volumeVendido);
        final var valorBrutoVenda = volumeVendido.multiply(precoAtual);
        final var taxa = valorBrutoVenda.multiply(TAXA_OPERACAO);
        final var valorLiquidoVenda = valorBrutoVenda.subtract(taxa);
        final var lucro = valorLiquidoVenda.subtract(custoTotalInvestido);

        if (Boolean.TRUE.equals(cache.vendaApenasPorLucro())) {
            final var percentualLucroObtido = lucro.divide(custoTotalInvestido, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
            if (percentualLucroObtido.compareTo(cache.percentualLucro()) < 0) {
                log.debug("[SIMULAÇÃO] Venda ignorada. Lucro {}% inferior ao alvo {}%", percentualLucroObtido, cache.percentualLucro());
                return;
            }
        }

        final var novoSaldo = cache.saldo().add(valorLiquidoVenda);

        operacaoCacheService.atualizarEstadoAposVenda(cache.id(), cache.par(), cache.intervalo(), novoSaldo);

        salvarVendaNoBanco(cache.id(), precoEntrada, precoAtual, lucro, novoSaldo, snapshotIndicadores, dataCandle);

        log.info("[SIMULAÇÃO] VENDA Executada. Lucro: {}. Novo Saldo: {}", lucro, novoSaldo);
    }

    private void salvarCompraNoBanco(String idOperacao, BigDecimal preco, BigDecimal valor, BigDecimal vol, BigDecimal saldo, String snapshot, long dataCandle) {
        final var optionalOperacao = operacaoRepository.findById(idOperacao);
        final var compra = new Compra();

        if (optionalOperacao.isPresent()) {
            final var operacao = optionalOperacao.get();

            compra.setOperacao(operacao);
            compra.setValorMoeda(preco);
            compra.setValorOperacao(valor);
            compra.setVolume(vol);
            compra.setDataCompra(DateUtils.agora());
            compra.setDataCandle(Instant.ofEpochSecond(dataCandle));
            compra.setSnapshotIndicadores(snapshot);

            compraRepository.save(compra);
            operacaoRepository.atualizarSaldo(idOperacao, saldo);
        }
    }

    private void salvarVendaNoBanco(String idOperacao, BigDecimal precoCompra, BigDecimal precoVenda, BigDecimal lucro, BigDecimal saldo, String snapshot, long dataCandle) {
        final var optionalOperacao = operacaoRepository.findById(idOperacao);
        final var venda = new Venda();

        if (optionalOperacao.isPresent()) {
            final var operacao = optionalOperacao.get();

            venda.setOperacao(operacao);
            venda.setValorCompra(precoCompra);
            venda.setValorVenda(precoVenda);
            venda.setLucro(lucro);
            venda.setDataVenda(DateUtils.agora());
            venda.setDataCandle(Instant.ofEpochSecond(dataCandle));
            venda.setSnapshotIndicadores(snapshot);

            vendaRepository.save(venda);
            operacaoRepository.atualizarSaldo(idOperacao, saldo);
        }
    }

    private BigDecimal calcularValorInvestimento(OperacaoCacheDTO cache, BigDecimal saldo) {
        if (cache.valorOperacaoFixo() != null && cache.valorOperacaoFixo().compareTo(BigDecimal.ZERO) > 0) {
            return cache.valorOperacaoFixo();
        }

        if (cache.percentualValorOperacao() != null && cache.percentualValorOperacao().compareTo(BigDecimal.ZERO) > 0) {
            return saldo.multiply(cache.percentualValorOperacao().divide(new BigDecimal("100"), 4, RoundingMode.DOWN));
        }

        return saldo;
    }

    private String serializarIndicadores(Map<String, BigDecimal> indicadores) {
        try {
            return indicadores != null ? objectMapper.writeValueAsString(indicadores) : "{}";
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar indicadores: {}", e.getMessage());
            return "{}";
        }
    }
}