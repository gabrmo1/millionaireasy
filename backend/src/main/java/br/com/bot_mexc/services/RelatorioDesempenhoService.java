package br.com.bot_mexc.services;

import br.com.bot_mexc.models.dtos.monitoramento.RelatorioDesempenhoDTO;
import br.com.bot_mexc.models.dtos.monitoramento.TradeMatchDTO;
import br.com.bot_mexc.models.entities.Compra;
import br.com.bot_mexc.models.entities.Operacao;
import br.com.bot_mexc.models.entities.Venda;
import br.com.bot_mexc.repositories.CandleRepository;
import br.com.bot_mexc.repositories.CompraRepository;
import br.com.bot_mexc.repositories.OperacaoRepository;
import br.com.bot_mexc.repositories.VendaRepository;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RelatorioDesempenhoService {

    private final OperacaoRepository operacaoRepository;
    private final CompraRepository compraRepository;
    private final VendaRepository vendaRepository;
    private final CandleRepository candleRepository;

    private static final BigDecimal TAXA_OPERACAO = new BigDecimal("0.001"); // 0.1%

    public RelatorioDesempenhoDTO gerarRelatorio(String operacaoId) {
        final Operacao operacao = operacaoRepository.findById(operacaoId)
                .orElseThrow(() -> new ValidationException("Operação não encontrada."));

        final List<TradeMatchDTO> trades = parearTrades(operacaoId);

        final BigDecimal saldoInicial = operacao.getSaldoInicial() != null
                ? operacao.getSaldoInicial()
                : BigDecimal.ZERO;

        return processarMotorEstatistico(trades, saldoInicial, operacao);
    }

    private List<TradeMatchDTO> parearTrades(String operacaoId) {
        final List<Compra> compras = compraRepository.findAllByOperacaoIdOrderByDataCompraAsc(operacaoId);
        final List<Venda> vendas = vendaRepository.findAllByOperacaoIdOrderByDataVendaAsc(operacaoId);

        final List<TradeMatchDTO> tradesPareados = new ArrayList<>(compras.size());

        for (int i = 0; i < compras.size(); i++) {
            Compra compra = compras.get(i);
            Venda venda = (i < vendas.size()) ? vendas.get(i) : null;
            tradesPareados.add(new TradeMatchDTO(compra, venda));
        }

        return tradesPareados;
    }

    /**
     * Motor de Cálculo Estatístico O(N) com Equity Curve e agregação de MAE/Custos.
     */
    private RelatorioDesempenhoDTO processarMotorEstatistico(List<TradeMatchDTO> trades, BigDecimal saldoInicial, Operacao operacao) {
        int vencedores = 0;
        int perdedores = 0;
        int zeradas = 0;

        BigDecimal lucroBruto = BigDecimal.ZERO;
        BigDecimal prejuizoBruto = BigDecimal.ZERO;
        BigDecimal maiorVitoria = BigDecimal.ZERO;
        BigDecimal maiorDerrota = BigDecimal.ZERO;
        BigDecimal custosTotais = BigDecimal.ZERO;

        long totalTempoSegundos = 0L;

        int currentWinStreak = 0;
        int maxWinStreak = 0;
        int currentLossStreak = 0;
        int maxLossStreak = 0;

        // Drawdown state
        BigDecimal saldoAtual = saldoInicial;
        BigDecimal picoMaximo = saldoInicial;
        BigDecimal maxDrawdownNominal = BigDecimal.ZERO;
        BigDecimal maxDrawdownPercentual = BigDecimal.ZERO;

        // MAE state
        BigDecimal somaMae = BigDecimal.ZERO;
        int contagemMae = 0;

        for (int i = 0; i < trades.size(); i++) {
            TradeMatchDTO trade = trades.get(i);

            if (trade.venda() == null) {
                zeradas++;
                continue;
            }

            final BigDecimal lucro = trade.venda().getLucro();
            final BigDecimal precoCompra = trade.compra().getValorMoeda();
            final BigDecimal precoVenda = trade.venda().getValorVenda();

            // 1. Custos (Mechanical Sympathy: Inferido sem onerar o BD)
            // Custo Compra = Volume(USDT) * 0.1%
            // Custo Venda = (Qtd Cripto * Preço Venda) * 0.1%
            final BigDecimal custoCompra = trade.compra().getValorOperacao().multiply(TAXA_OPERACAO);
            final BigDecimal volumeVendaCotizada = trade.compra().getVolume().multiply(precoVenda);
            final BigDecimal custoVenda = volumeVendaCotizada.multiply(TAXA_OPERACAO);
            custosTotais = custosTotais.add(custoCompra).add(custoVenda);

            totalTempoSegundos += Duration.between(trade.compra().getDataCompra(), trade.venda().getDataVenda()).getSeconds();

            // 2. Equity Curve e High Water Mark
            saldoAtual = saldoAtual.add(lucro).subtract(custoCompra).subtract(custoVenda);

            if (saldoAtual.compareTo(picoMaximo) > 0) {
                picoMaximo = saldoAtual;
            } else {
                BigDecimal drawdownNominal = picoMaximo.subtract(saldoAtual);
                if (drawdownNominal.compareTo(maxDrawdownNominal) > 0) {
                    maxDrawdownNominal = drawdownNominal;
                }

                if (picoMaximo.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal drawdownPercentual = drawdownNominal
                            .divide(picoMaximo, 6, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP);

                    if (drawdownPercentual.compareTo(maxDrawdownPercentual) > 0) {
                        maxDrawdownPercentual = drawdownPercentual;
                    }
                }
            }

            // 3. Lucros e Sequências
            if (lucro.compareTo(BigDecimal.ZERO) > 0) {
                vencedores++;
                lucroBruto = lucroBruto.add(lucro);
                if (lucro.compareTo(maiorVitoria) > 0) maiorVitoria = lucro;

                currentWinStreak++;
                currentLossStreak = 0;
                if (currentWinStreak > maxWinStreak) maxWinStreak = currentWinStreak;
            } else {
                perdedores++;
                prejuizoBruto = prejuizoBruto.add(lucro);
                if (lucro.compareTo(maiorDerrota) < 0) maiorDerrota = lucro;

                currentLossStreak++;
                currentWinStreak = 0;
                if (currentLossStreak > maxLossStreak) maxLossStreak = currentLossStreak;

                // 4. MAE (Máxima Excursão Adversa) - Foco nos trades perdedores para otimizar query OOM
                BigDecimal minPrice = candleRepository.findMinPriceBetween(
                        operacao.getPar(),
                        operacao.getIntervalo(),
                        trade.compra().getDataCompra(),
                        trade.venda().getDataVenda()
                );

                if (minPrice != null && precoCompra.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal mae = precoCompra.subtract(minPrice)
                            .divide(precoCompra, 6, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));

                    if (mae.compareTo(BigDecimal.ZERO) > 0) {
                        somaMae = somaMae.add(mae);
                        contagemMae++;
                    }
                }
            }
        }

        final int totalFechados = vencedores + perdedores;

        BigDecimal fatorLucro = BigDecimal.ZERO;
        if (prejuizoBruto.compareTo(BigDecimal.ZERO) == 0) {
            fatorLucro = lucroBruto.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(999) : BigDecimal.ZERO;
        } else {
            fatorLucro = lucroBruto.divide(prejuizoBruto.abs(), 4, RoundingMode.HALF_UP);
        }

        final BigDecimal maeMedioPercentual = contagemMae > 0
                ? somaMae.divide(BigDecimal.valueOf(contagemMae), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        final String tempoMedioOperacao = totalFechados > 0
                ? Duration.ofSeconds(totalTempoSegundos / totalFechados).toString()
                : "PT0S";

        // Lucro Líquido = Lucro Bruto + Prejuízo Bruto (já negativo) - Custos Totais
        final BigDecimal lucroLiquido = lucroBruto.add(prejuizoBruto).subtract(custosTotais).setScale(4, RoundingMode.HALF_UP);

        return new RelatorioDesempenhoDTO(
                trades.isEmpty() ? null : trades.getFirst().compra().getDataCompra(),
                trades.isEmpty() ? null : (trades.getLast().venda() != null ? trades.getLast().venda().getDataVenda() : trades.getLast().compra().getDataCompra()),
                totalFechados,
                vencedores,
                perdedores,
                zeradas,
                lucroBruto.setScale(4, RoundingMode.HALF_UP),
                prejuizoBruto.setScale(4, RoundingMode.HALF_UP),
                lucroLiquido,
                fatorLucro,
                maxDrawdownPercentual,
                maxDrawdownNominal,
                maeMedioPercentual,
                maiorVitoria,
                maiorDerrota,
                tempoMedioOperacao,
                maxWinStreak,
                maxLossStreak
        );
    }
}