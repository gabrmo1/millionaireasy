package br.com.bot_mexc.modules.strategy.consumers;
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
import br.com.bot_mexc.modules.timeseries.entities.Analise;
import br.com.bot_mexc.modules.timeseries.services.BacktestCandleProviderService;
import br.com.bot_mexc.modules.timeseries.dtos.CandleDTO;
import br.com.bot_mexc.modules.timeseries.builders.AnaliseBuilder;

import br.com.bot_mexc.modules.timeseries.builders.AnaliseBuilder;
import br.com.bot_mexc.shared.configs.RabbitMQConfig;
import br.com.bot_mexc.modules.timeseries.dtos.CandleDTO;
import br.com.bot_mexc.modules.strategy.dtos.IndicadorConfigDTO;
import br.com.bot_mexc.modules.strategy.dtos.PrimitiveCandle;

import br.com.bot_mexc.shared.enums.StatusOperacoes;
import br.com.bot_mexc.modules.timeseries.repositories.AnaliseRepository;
import br.com.bot_mexc.modules.strategy.repositories.CompraRepository;
import br.com.bot_mexc.modules.strategy.repositories.OperacaoRepository;
import br.com.bot_mexc.modules.strategy.repositories.VendaRepository;
import br.com.bot_mexc.modules.strategy.services.AvaliacaoCondicaoService;
import br.com.bot_mexc.modules.timeseries.services.BacktestCandleProviderService;
import br.com.bot_mexc.modules.strategy.services.indicators.IndicadorContext;
import br.com.bot_mexc.modules.strategy.services.indicators.IndicadorState;
import br.com.bot_mexc.modules.strategy.services.indicators.IndicadorStrategy;
import br.com.bot_mexc.modules.strategy.services.indicators.IndicadorStrategyRegistry;
import br.com.bot_mexc.shared.constants.IndicadorKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class BacktestEngineConsumer {

    private final OperacaoRepository operacaoRepository;
    private final CompraRepository compraRepository;
    private final VendaRepository vendaRepository;
    private final AnaliseRepository analiseRepository;
    private final BacktestCandleProviderService candleProviderService;
    private final AvaliacaoCondicaoService avaliacaoCondicaoService;
    private final IndicadorStrategyRegistry strategyRegistry;
    private final ObjectMapper objectMapper;
    private static final BigDecimal TAXA_OPERACAO = new BigDecimal("0.001"); // 0.1%

    @RabbitListener(queues = RabbitMQConfig.SIMULATIONS_PROCESS_QUEUE)
    public void processarBacktest(String operacaoId) {
        log.info("[BACKTEST-ENGINE] Iniciando processamento da simulação: {}", operacaoId);
        Operacao operacao = operacaoRepository.findByIdEagerly(operacaoId).orElse(null);
        if (operacao == null) {
            try {
                Thread.sleep(150);
            } catch (InterruptedException ignored) {}
            operacao = operacaoRepository.findByIdEagerly(operacaoId).orElse(null);
        }

        if (operacao == null || operacao.getStatus() != StatusOperacoes.AGUARDANDO) {
            log.warn("[BACKTEST-ENGINE] Operação nula ou em status inválido. Abortando. Status: {}", operacao != null ? operacao.getStatus() : "NULL");
            return;
        }

        try {
            operacao.setStatus(StatusOperacoes.EM_ANDAMENTO);
            operacaoRepository.save(operacao);

            Estrategia estrategia = operacao.getEstrategia();
            Set<IndicadorConfig> configs = estrategia.getIndicadoresConfig();
            List<CondicaoCompra> condicoesCompra = new ArrayList<>(estrategia.getCondicoesCompra());
            List<CondicaoVenda> condicoesVenda = new ArrayList<>(estrategia.getCondicoesVenda());

            List<CandleDTO> candles = candleProviderService.obterCandles(
                    operacao.getPar(),
                    operacao.getIntervalo(),
                    operacao.getDataInicio(),
                    operacao.getDataFim()
            );

            if (candles.isEmpty()) {
                throw new RuntimeException("Nenhum candle retornado para o período selecionado.");
            }

            int initialCapacity = candles.size();
            List<Compra> bufferCompras = new ArrayList<>();
            List<Venda> bufferVendas = new ArrayList<>();
            List<Analise> bufferAnalises = new ArrayList<>(initialCapacity);

            BigDecimal saldo = operacao.getSaldoInicial();
            BigDecimal volumeEmMao = BigDecimal.ZERO;
            BigDecimal precoMedioEntrada = BigDecimal.ZERO;
            boolean posicionado = false;

            // Pipeline pré-alocado (Simpatia Mecânica)
            // Evita hash lookups no loop crítico com Arrays paralelos (Acesso L1 Cache sequencial)
            int numConfigs = configs.size();
            IndicadorConfig[] configArray = configs.toArray(new IndicadorConfig[0]);

            IndicadorStrategy[] strategyArray = new IndicadorStrategy[numConfigs];
            IndicadorState[] stateArray = new IndicadorState[numConfigs];
            @SuppressWarnings("unchecked")
            Map<String, Integer>[] paramsArray = new Map[numConfigs];

            for (int i = 0; i < numConfigs; i++) {
                // Recupera a estratégia com 100% de garantia (sem null check)
                strategyArray[i] = strategyRegistry.get(configArray[i].getTipoIndicador());

                // Sem risco de NullPointerException
                stateArray[i] = strategyArray[i].inicializarEstado();

                paramsArray[i] = IndicadorConfigDTO.parametrosFromJson(configArray[i].getParametros());
            }

            double[] currentValues = new double[numConfigs];
            double[] previousValues = new double[numConfigs];
            Arrays.fill(previousValues, Double.NaN);
            double previousClose = Double.NaN;

            int maxWarmup = 14;
            for (int k = 0; k < numConfigs; k++) {
                if (paramsArray[k] != null) {
                    for (Integer val : paramsArray[k].values()) {
                        if (val != null && val > maxWarmup && val < 500) {
                            maxWarmup = Math.max(maxWarmup, val);
                        }
                    }
                }
            }
            int warmupCandles = Math.min(maxWarmup, Math.max(0, candles.size() - 1));

            // Loop Temporal: Iteração CPU-bound Pura (Zero Alocações na Fase de Warmup)
            for (int i = 0; i < candles.size(); i++) {
                CandleDTO candle = candles.get(i);

                // Conversão escalar ultra-rápida (JIT Escape Analysis)
                PrimitiveCandle pCandle = new PrimitiveCandle(
                        candle.dataAbertura(), candle.dataFechamento(),
                        candle.valorAbertura().doubleValue(), candle.valorFechamento().doubleValue(),
                        candle.minima().doubleValue(), candle.maxima().doubleValue(), candle.volume().doubleValue()
                );

                // Cálculo in-place nos buffers L1
                for (int j = 0; j < numConfigs; j++) {
                    IndicadorContext ctx = new IndicadorContext(paramsArray[j], stateArray[j], previousClose, null);
                    currentValues[j] = strategyArray[j].calcular(pCandle, ctx);
                }

                // Avaliação apenas após o Warmup (150 candles) para economizar GC com BigDecimals
                if (i >= warmupCandles) {
                    Map<String, BigDecimal> indicadoresResultados = new HashMap<>(numConfigs * 2 + 2, 1.0f);

                    indicadoresResultados.put(IndicadorKeys.RESULT_PRECO_FECHAMENTO, candle.valorFechamento());
                    if (!Double.isNaN(previousClose)) {
                        indicadoresResultados.put(IndicadorKeys.RESULT_PREVIOUS_PREFIX + IndicadorKeys.RESULT_PRECO_FECHAMENTO, BigDecimal.valueOf(previousClose));
                    }

                    for (int j = 0; j < numConfigs; j++) {
                        String alias = configArray[j].getAlias();
                        double val = currentValues[j];
                        double prevVal = previousValues[j];

                        indicadoresResultados.put(alias, Double.isNaN(val) ? BigDecimal.ZERO : BigDecimal.valueOf(val));
                        if (!Double.isNaN(prevVal)) {
                            indicadoresResultados.put(IndicadorKeys.RESULT_PREVIOUS_PREFIX + alias, BigDecimal.valueOf(prevVal));
                        }
                    }

                    if (!posicionado) {
                        boolean sinalCompra = avaliacaoCondicaoService.avaliarCondicoesCompra(condicoesCompra, indicadoresResultados);
                        if (sinalCompra) {
                            BigDecimal valorInvestimento = calcularValorInvestimento(estrategia, saldo);
                            if (valorInvestimento.compareTo(new BigDecimal("5")) >= 0 && saldo.compareTo(valorInvestimento) >= 0) {
                                BigDecimal volumeComprado = valorInvestimento.divide(candle.valorFechamento(), 8, RoundingMode.DOWN);
                                BigDecimal volumeLiquido = volumeComprado.subtract(volumeComprado.multiply(TAXA_OPERACAO));

                                saldo = saldo.subtract(valorInvestimento);
                                volumeEmMao = volumeLiquido;
                                precoMedioEntrada = candle.valorFechamento();
                                posicionado = true;

                                Compra compra = new Compra();
                                compra.setOperacao(operacao);
                                compra.setValorMoeda(candle.valorFechamento());
                                compra.setValorOperacao(valorInvestimento);
                                compra.setVolume(volumeLiquido);
                                compra.setDataCompra(Instant.ofEpochSecond(candle.dataAbertura()));
                                compra.setDataCandle(Instant.ofEpochSecond(candle.dataAbertura()));
                                compra.setSnapshotIndicadores(objectMapper.writeValueAsString(indicadoresResultados));
                                bufferCompras.add(compra);
                            }
                        }
                    } else {
                        boolean sinalVenda = avaliacaoCondicaoService.avaliarCondicoesVenda(condicoesVenda, indicadoresResultados);
                        if (sinalVenda) {
                            BigDecimal custoTotalInvestido = precoMedioEntrada.multiply(volumeEmMao);
                            BigDecimal valorBrutoVenda = volumeEmMao.multiply(candle.valorFechamento());
                            BigDecimal taxa = valorBrutoVenda.multiply(TAXA_OPERACAO);
                            BigDecimal valorLiquidoVenda = valorBrutoVenda.subtract(taxa);
                            BigDecimal lucro = valorLiquidoVenda.subtract(custoTotalInvestido);

                            boolean executarVenda = true;
                            if (Boolean.TRUE.equals(estrategia.getVendaApenasPorLucro())) {
                                BigDecimal percentualLucroObtido = lucro.divide(custoTotalInvestido, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
                                if (percentualLucroObtido.compareTo(estrategia.getPercentualLucro()) < 0) {
                                    executarVenda = false;
                                }
                            }

                            if (executarVenda) {
                                saldo = saldo.add(valorLiquidoVenda);

                                Venda venda = new Venda();
                                venda.setOperacao(operacao);
                                venda.setValorCompra(precoMedioEntrada);
                                venda.setValorVenda(candle.valorFechamento());
                                venda.setLucro(lucro);
                                venda.setDataVenda(Instant.ofEpochSecond(candle.dataAbertura()));
                                venda.setDataCandle(Instant.ofEpochSecond(candle.dataAbertura()));
                                venda.setSnapshotIndicadores(objectMapper.writeValueAsString(indicadoresResultados));
                                bufferVendas.add(venda);

                                posicionado = false;
                                volumeEmMao = BigDecimal.ZERO;
                                precoMedioEntrada = BigDecimal.ZERO;
                            }
                        }
                    }

                    // Bufferização com atraso de instância para Monitoramento (Task OOM prevenida)
                    Analise analise = AnaliseBuilder.montarAnalise(
                            operacao.getPar(),
                            operacao.getIntervalo(),
                            candle,
                            configs,
                            indicadoresResultados
                    );
                    analise.setIdUsuario(operacao.getIdUsuario());
                    bufferAnalises.add(analise);
                }

                // Commit de estado para o próximo tick copiando os vetores no L1 Cache
                previousClose = pCandle.valorFechamento();
                System.arraycopy(currentValues, 0, previousValues, 0, numConfigs);
            }

            // Task 3.4 - Aciona I/O
            salvarEmLote(operacao, bufferCompras, bufferVendas, bufferAnalises, saldo);

        } catch (Exception e) {
            log.error("[BACKTEST-ENGINE] Falha catastrófica na simulação {}", operacaoId, e);
            operacao.setStatus(StatusOperacoes.ERRO);
            operacaoRepository.save(operacao);
        }
    }

    @Transactional
    protected void salvarEmLote(Operacao operacao, List<Compra> compras, List<Venda> vendas, List<Analise> analises, BigDecimal saldoFinal) {
        log.info("[BACKTEST-ENGINE] Salvando buffers: {} Compras, {} Vendas, {} Analises", compras.size(), vendas.size(), analises.size());

        final int batchSize = 10000;
        for (int i = 0; i < analises.size(); i += batchSize) {
            int end = Math.min(analises.size(), i + batchSize);
            analiseRepository.saveAll(analises.subList(i, end));
        }

        compraRepository.saveAll(compras);
        vendaRepository.saveAll(vendas);

        operacao.setStatus(StatusOperacoes.FINALIZADO);
        operacao.setSaldoInicial(saldoFinal);
        operacao.setDataFim(Instant.now());
        operacaoRepository.save(operacao);

        log.info("[BACKTEST-ENGINE] Simulação {} FINALIZADA com sucesso. Saldo final: {}", operacao.getId(), saldoFinal);
    }

    private BigDecimal calcularValorInvestimento(Estrategia cache, BigDecimal saldo) {
        if (cache.getValorOperacaoFixo() != null && cache.getValorOperacaoFixo().compareTo(BigDecimal.ZERO) > 0) {
            return cache.getValorOperacaoFixo();
        }
        if (cache.getPercentualValorOperacao() != null && cache.getPercentualValorOperacao().compareTo(BigDecimal.ZERO) > 0) {
            return saldo.multiply(cache.getPercentualValorOperacao().divide(new BigDecimal("100"), 4, RoundingMode.DOWN));
        }
        return saldo;
    }
}