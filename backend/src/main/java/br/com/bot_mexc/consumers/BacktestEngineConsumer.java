package br.com.bot_mexc.consumers;

import br.com.bot_mexc.builders.AnaliseBuilder;
import br.com.bot_mexc.configs.RabbitMQConfig;
import br.com.bot_mexc.models.dtos.CandleDTO;
import br.com.bot_mexc.models.dtos.IndicadorConfigDTO;
import br.com.bot_mexc.models.entities.*;
import br.com.bot_mexc.models.enums.StatusOperacoes;
import br.com.bot_mexc.models.enums.TipoIndicador;
import br.com.bot_mexc.repositories.*;
import br.com.bot_mexc.services.AvaliacaoCondicaoService;
import br.com.bot_mexc.services.BacktestCandleProviderService;
import br.com.bot_mexc.utils.CalculoUtils;
import br.com.bot_mexc.utils.constants.IndicadorKeys;
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
    private final ObjectMapper objectMapper;

    private static final BigDecimal TAXA_OPERACAO = new BigDecimal("0.001"); // 0.1%

    @RabbitListener(queues = RabbitMQConfig.SIMULATIONS_PROCESS_QUEUE)
    public void processarBacktest(String operacaoId) {
        log.info("[BACKTEST-ENGINE] Iniciando processamento da simulação: {}", operacaoId);

        Operacao operacao = operacaoRepository.findByIdEagerly(operacaoId).orElse(null);

        if (operacao == null || operacao.getStatus() != StatusOperacoes.AGUARDANDO) {
            log.warn("[BACKTEST-ENGINE] Operação nula ou em status inválido. Abortando.");
            return;
        }

        try {
            // Task 3.2: Inicia estado de processamento
            operacao.setStatus(StatusOperacoes.EM_ANDAMENTO);
            operacaoRepository.save(operacao);

            // Pré-carregamento para bypassar lazy-loading dentro do loop
            Estrategia estrategia = operacao.getEstrategia();
            Set<IndicadorConfig> configs = estrategia.getIndicadoresConfig();
            List<CondicaoCompra> condicoesCompra = new ArrayList<>(estrategia.getCondicoesCompra());
            List<CondicaoVenda> condicoesVenda = new ArrayList<>(estrategia.getCondicoesVenda());

            // Motor Híbrido: Fetch e unificação contígua (O(1) iteration ready)
            List<CandleDTO> candles = candleProviderService.obterCandles(
                    operacao.getPar(),
                    operacao.getIntervalo(),
                    operacao.getDataInicio(),
                    operacao.getDataFim()
            );

            if (candles.isEmpty()) {
                throw new RuntimeException("Nenhum candle retornado para o período selecionado.");
            }

            // Alocação otimizada (Mechanical Sympathy)
            int initialCapacity = candles.size();
            List<Compra> bufferCompras = new ArrayList<>();
            List<Venda> bufferVendas = new ArrayList<>();
            List<Analise> bufferAnalises = new ArrayList<>(initialCapacity);

            BigDecimal saldo = operacao.getSaldoInicial();
            BigDecimal volumeEmMao = BigDecimal.ZERO;
            BigDecimal precoMedioEntrada = BigDecimal.ZERO;
            boolean posicionado = false;

            // Trackers na Heap local (Substitui Redis)
            Map<String, IndicadorTracker> trackers = inicializarTrackers(configs);
            BigDecimal previousClose = null;
            Map<String, BigDecimal> indicadoresResultados = new HashMap<>(configs.size() + 2, 1.0f);

            // Variável deslizante para médias
            List<BigDecimal> precosFechamento = new ArrayList<>(initialCapacity);

            // Loop Temporal: Iteração CPU-bound Pura
            for (int i = 0; i < candles.size(); i++) {
                CandleDTO candle = candles.get(i);
                precosFechamento.add(candle.valorFechamento());

                indicadoresResultados.clear();
                indicadoresResultados.put(IndicadorKeys.RESULT_PRECO_FECHAMENTO, candle.valorFechamento());
                if (previousClose != null) {
                    indicadoresResultados.put(IndicadorKeys.RESULT_PREVIOUS_PREFIX + IndicadorKeys.RESULT_PRECO_FECHAMENTO, previousClose);
                }

                // Carga de indicadores incremental $O(1)$
                for (IndicadorConfig config : configs) {
                    IndicadorTracker tracker = trackers.get(config.getAlias());
                    BigDecimal valor = tracker.calcularTick(candle.valorFechamento(), previousClose, precosFechamento);
                    indicadoresResultados.put(config.getAlias(), valor);
                    if (tracker.getPreviousValue() != null) {
                        indicadoresResultados.put(IndicadorKeys.RESULT_PREVIOUS_PREFIX + config.getAlias(), tracker.getPreviousValue());
                    }
                }

                // Task 3.3: Avaliação (Aguarda 150 candles de warmup para estabilizar médias/osciladores)
                if (i > 150) {
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
                }

                // Bufferização de Análise Técnica
                Analise analise = AnaliseBuilder.montarAnalise(
                        operacao.getPar(),
                        operacao.getIntervalo(),
                        candle,
                        configs,
                        indicadoresResultados
                );
                analise.setIdUsuario(operacao.getIdUsuario());
                bufferAnalises.add(analise);

                // Commit de estado para o próximo tick
                previousClose = candle.valorFechamento();
                for (IndicadorConfig config : configs) {
                    trackers.get(config.getAlias()).commitTick();
                }
            } // Fim CPU-bound Loop

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

        // Chunking Insert (Evita out of memory e statement timeout no PGSQL)
        final int batchSize = 10000;
        for (int i = 0; i < analises.size(); i += batchSize) {
            int end = Math.min(analises.size(), i + batchSize);
            analiseRepository.saveAll(analises.subList(i, end));
        }

        compraRepository.saveAll(compras);
        vendaRepository.saveAll(vendas);

        operacao.setStatus(StatusOperacoes.FINALIZADO);
        operacao.setSaldoInicial(saldoFinal); // Permite ao frontend plotar o capital ganho com query simples
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

    private Map<String, IndicadorTracker> inicializarTrackers(Set<IndicadorConfig> configs) {
        Map<String, IndicadorTracker> trackers = new HashMap<>(configs.size(), 1.0f);
        for (IndicadorConfig config : configs) {
            Map<String, Integer> params = IndicadorConfigDTO.parametrosFromJson(config.getParametros());
            trackers.put(config.getAlias(), new IndicadorTracker(config.getTipoIndicador(), params));
        }
        return trackers;
    }

    /**
     * Classe encapsulada para manter controle de estado imutável sem alocação do Redis.
     */
    private static class IndicadorTracker {
        private final TipoIndicador tipo;
        private final Map<String, Integer> params;

        private BigDecimal currentValue = BigDecimal.ZERO;
        private BigDecimal previousValue = BigDecimal.ZERO;
        private BigDecimal emaAnterior = null;
        private BigDecimal avgGain = BigDecimal.ZERO;
        private BigDecimal avgLoss = BigDecimal.ZERO;

        public IndicadorTracker(TipoIndicador tipo, Map<String, Integer> params) {
            this.tipo = tipo;
            this.params = params;
        }

        public BigDecimal calcularTick(BigDecimal precoAtual, BigDecimal precoAnterior, List<BigDecimal> precosHistoricos) {
            try {
                switch (tipo) {
                    case EMA:
                        int pEma = params.getOrDefault(IndicadorKeys.PARAM_PERIODO_EMA, 200);
                        if (emaAnterior == null && precosHistoricos.size() >= pEma) {
                            emaAnterior = CalculoUtils.calcularEma(precosHistoricos, pEma);
                            currentValue = emaAnterior;
                        } else if (emaAnterior != null) {
                            currentValue = CalculoUtils.calcularEmaIncremental(precoAtual, emaAnterior, pEma);
                        }
                        break;
                    case SMA:
                        int pSma = params.getOrDefault(IndicadorKeys.PARAM_PERIODO_SMA, 200);
                        if (precosHistoricos.size() >= pSma) {
                            currentValue = CalculoUtils.calcularSma(precosHistoricos, pSma);
                        }
                        break;
                    case RSI_CURTO:
                    case RSI_MEDIO:
                    case RSI_LONGO:
                        int pRsi = tipo == TipoIndicador.RSI_CURTO ? params.getOrDefault(IndicadorKeys.PARAM_PERIODO_RSI_CURTO, 7) :
                                tipo == TipoIndicador.RSI_MEDIO ? params.getOrDefault(IndicadorKeys.PARAM_PERIODO_RSI_MEDIO, 14) :
                                        params.getOrDefault(IndicadorKeys.PARAM_PERIODO_RSI_LONGO, 21);

                        if (precoAnterior != null) {
                            BigDecimal variacao = precoAtual.subtract(precoAnterior);
                            BigDecimal ganho = variacao.compareTo(BigDecimal.ZERO) > 0 ? variacao : BigDecimal.ZERO;
                            BigDecimal perda = variacao.compareTo(BigDecimal.ZERO) < 0 ? variacao.abs() : BigDecimal.ZERO;

                            if (precosHistoricos.size() <= pRsi) {
                                avgGain = avgGain.add(ganho).divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_UP);
                                avgLoss = avgLoss.add(perda).divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_UP);
                            } else {
                                avgGain = CalculoUtils.calcularMediaGanhoRsi(avgGain, pRsi, ganho);
                                avgLoss = CalculoUtils.calcularMediaPerdaRsi(avgLoss, pRsi, perda);
                            }
                            currentValue = CalculoUtils.calculateRsiFromAverages(avgGain, avgLoss);
                        }
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                currentValue = BigDecimal.ZERO;
            }
            return currentValue;
        }

        public void commitTick() {
            previousValue = currentValue;
            if (tipo == TipoIndicador.EMA) {
                emaAnterior = currentValue;
            }
        }

        public BigDecimal getPreviousValue() {
            return previousValue;
        }
    }
}