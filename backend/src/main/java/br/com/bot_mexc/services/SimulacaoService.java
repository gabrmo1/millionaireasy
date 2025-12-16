package br.com.bot_mexc.services;

import br.com.bot_mexc.models.dtos.OperacaoCacheDTO;
import br.com.bot_mexc.models.dtos.OrdemRequestDTO;
import br.com.bot_mexc.models.entities.Compra;
import br.com.bot_mexc.models.entities.Operacao;
import br.com.bot_mexc.models.entities.Venda;
import br.com.bot_mexc.repositories.CompraRepository;
import br.com.bot_mexc.repositories.OperacaoRepository;
import br.com.bot_mexc.repositories.VendaRepository;
import br.com.bot_mexc.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimulacaoService {

    private final OperacaoRepository operacaoRepository;
    private final CompraRepository compraRepository;
    private final VendaRepository vendaRepository;
    private final OperacaoCacheService operacaoCacheService;

    private static final BigDecimal TAXA_OPERACAO = new BigDecimal("0.001"); // 0.1%

    public void processarOrdemSimulada(OperacaoCacheDTO operacaoCache, OrdemRequestDTO ordem) {
        if (ordem.tipo() == OrdemRequestDTO.TipoOrdem.BUY) {
            executarCompraSimulada(operacaoCache, ordem.preco());
        } else {
            executarVendaSimulada(operacaoCache, ordem.preco());
        }
    }

    private void executarCompraSimulada(OperacaoCacheDTO cache, BigDecimal precoAtual) {
        if (cache.posicionado()) {
            return;
        }

        BigDecimal saldoDisponivel = cache.saldo();
        BigDecimal valorInvestimento = calcularValorInvestimento(cache, saldoDisponivel);

        if (valorInvestimento.compareTo(new BigDecimal("5")) < 0) {
            log.warn("[SIMULAÇÃO] Saldo insuficiente ou valor de operação muito baixo para {}: {}", cache.id(), valorInvestimento);
            return;
        }

        if (saldoDisponivel.compareTo(valorInvestimento) < 0) {
            valorInvestimento = saldoDisponivel;
        }

        BigDecimal volumeComprado = valorInvestimento.divide(precoAtual, 8, RoundingMode.DOWN);
        BigDecimal volumeLiquido = volumeComprado.subtract(volumeComprado.multiply(TAXA_OPERACAO));
        BigDecimal novoSaldo = saldoDisponivel.subtract(valorInvestimento);

        operacaoCacheService.atualizarEstadoAposCompra(cache.id(), cache.par(), cache.intervalo(), novoSaldo, precoAtual, volumeLiquido);

        salvarCompraNoBanco(cache.id(), precoAtual, valorInvestimento, volumeLiquido, novoSaldo);

        log.info("[SIMULAÇÃO] COMPRA Executada. Vol: {} @ {}. Saldo Restante: {}", volumeLiquido, precoAtual, novoSaldo);
    }

    private void executarVendaSimulada(OperacaoCacheDTO cache, BigDecimal precoAtual) {
        if (!cache.posicionado()) {
            return;
        }

        BigDecimal volumeVendido = cache.volumeEmMao();
        BigDecimal precoEntrada = cache.precoMedioEntrada();

        BigDecimal custoTotalInvestido = precoEntrada.multiply(volumeVendido);

        BigDecimal valorBrutoVenda = volumeVendido.multiply(precoAtual);
        BigDecimal taxa = valorBrutoVenda.multiply(TAXA_OPERACAO);
        BigDecimal valorLiquidoVenda = valorBrutoVenda.subtract(taxa);

        BigDecimal lucro = valorLiquidoVenda.subtract(custoTotalInvestido);

        if (Boolean.TRUE.equals(cache.vendaApenasPorLucro())) {
            BigDecimal percentualLucroObtido = lucro.divide(custoTotalInvestido, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
            if (percentualLucroObtido.compareTo(cache.percentualLucro()) < 0) {
                log.debug("[SIMULAÇÃO] Venda ignorada. Lucro {}% inferior ao alvo {}%", percentualLucroObtido, cache.percentualLucro());
                return;
            }
        }

        BigDecimal novoSaldo = cache.saldo().add(valorLiquidoVenda);

        operacaoCacheService.atualizarEstadoAposVenda(cache.id(), cache.par(), cache.intervalo(), novoSaldo);

        salvarVendaNoBanco(cache.id(), precoEntrada, precoAtual, lucro, novoSaldo);

        log.info("[SIMULAÇÃO] VENDA Executada. Lucro: {}. Novo Saldo: {}", lucro, novoSaldo);
    }

    private void salvarCompraNoBanco(String opId, BigDecimal preco, BigDecimal valor, BigDecimal vol, BigDecimal saldo) {
        Operacao opProxy = operacaoRepository.getReferenceById(opId);

        Compra compra = new Compra();
        compra.setOperacao(opProxy);
        compra.setValor_moeda(preco);
        compra.setValor_operacao(valor);
        compra.setVolume(vol);
        compra.setData_compra(DateUtils.agora());

        compraRepository.save(compra);
        operacaoRepository.atualizarSaldo(opId, saldo);
    }

    private void salvarVendaNoBanco(String opId, BigDecimal precoCompra, BigDecimal precoVenda, BigDecimal lucro, BigDecimal saldo) {
        Operacao opProxy = operacaoRepository.getReferenceById(opId);

        Venda venda = new Venda();
        venda.setOperacao(opProxy);
        venda.setValorCompra(precoCompra);
        venda.setValorVenda(precoVenda);
        venda.setLucro(lucro);
        venda.setDataVenda(DateUtils.agora());

        vendaRepository.save(venda);
        operacaoRepository.atualizarSaldo(opId, saldo);
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
}