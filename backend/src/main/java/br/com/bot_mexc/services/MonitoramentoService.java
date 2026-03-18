package br.com.bot_mexc.services;

import br.com.bot_mexc.models.dtos.CandleDTO;
import br.com.bot_mexc.models.dtos.monitoramento.CandleChartDTO;
import br.com.bot_mexc.models.dtos.monitoramento.EventoChartDTO;
import br.com.bot_mexc.models.dtos.monitoramento.IndicadorPointDTO;
import br.com.bot_mexc.models.dtos.monitoramento.MonitoramentoDataDTO;
import br.com.bot_mexc.models.entities.Analise;
import br.com.bot_mexc.models.entities.IndicadorConfig;
import br.com.bot_mexc.models.entities.Operacao;
import br.com.bot_mexc.models.entities.Venda;
import br.com.bot_mexc.models.enums.TipoIndicador;
import br.com.bot_mexc.repositories.AnaliseRepository;
import br.com.bot_mexc.repositories.CompraRepository;
import br.com.bot_mexc.repositories.OperacaoRepository;
import br.com.bot_mexc.repositories.VendaRepository;
import br.com.bot_mexc.utils.constants.IndicadorKeys;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

@Service
public class MonitoramentoService {

    private final OperacaoRepository operacaoRepository;
    private final MexcService mexcService;
    private final AnaliseRepository analiseRepository;
    private final CompraRepository compraRepository;
    private final VendaRepository vendaRepository;
    private final ObjectMapper objectMapper;

    public MonitoramentoService(OperacaoRepository operacaoRepository, MexcService mexcService,
                                AnaliseRepository analiseRepository, CompraRepository compraRepository,
                                VendaRepository vendaRepository, ObjectMapper objectMapper) {
        this.operacaoRepository = operacaoRepository;
        this.mexcService = mexcService;
        this.analiseRepository = analiseRepository;
        this.compraRepository = compraRepository;
        this.vendaRepository = vendaRepository;
        this.objectMapper = objectMapper;
    }

    public MonitoramentoDataDTO buscarDadosMonitoramento(String operacaoId) {
        final var operacao = operacaoRepository.findById(operacaoId).orElseThrow(() -> new RuntimeException("Operação não encontrada"));
        final var candlesMexc = mexcService.consultarCandles(operacao.getPar(), operacao.getIntervalo(), "1000");

        if (candlesMexc.isEmpty())
            return buildEmptyDTO(operacao);

        var candleChartDTOS = new ArrayList<CandleChartDTO>();
        var dataMin = (Instant) null;
        var dataMax = (Instant) null;

        for (CandleDTO c : candlesMexc) {
            final var dataCandle = c.dataAbertura();

            if (dataMin == null || dataCandle.isBefore(dataMin))
                dataMin = dataCandle;

            if (dataMax == null || dataCandle.isAfter(dataMax))
                dataMax = dataCandle;

            final var timeSeconds = dataCandle.atZone(ZoneId.systemDefault()).toEpochSecond();

            candleChartDTOS.add(CandleChartDTO.builder()
                    .time(timeSeconds)
                    .open(c.valorAbertura())
                    .high(c.maxima())
                    .low(c.minima())
                    .close(c.valorFechamento())
                    .volume(c.volume())
                    .build());
        }

        final var params = extrairParametrosEstrategia(operacao.getEstrategia().getIndicadoresConfig());
        final var analises = analiseRepository.buscarAnalisesCompativeis(
                operacao.getPar(),
                operacao.getIntervalo(),
                dataMin,
                dataMax,
                params.getOrDefault(IndicadorKeys.KEY_RSI_CURTO, 0),
                params.getOrDefault(IndicadorKeys.KEY_RSI_MEDIO, 0),
                params.getOrDefault(IndicadorKeys.KEY_RSI_LONGO, 0),
                params.getOrDefault(IndicadorKeys.KEY_RSI_STOCH, 0),
                params.getOrDefault(IndicadorKeys.KEY_STOCH_K, 0),
                params.getOrDefault(IndicadorKeys.KEY_STOCH_D, 0),
                params.get(IndicadorKeys.KEY_EMA),
                params.get(IndicadorKeys.KEY_SMA)
        );

        final var indicadoresMap = new HashMap<String, List<IndicadorPointDTO>>();

        Arrays.asList(
                IndicadorKeys.NAME_EMA, IndicadorKeys.NAME_SMA,
                IndicadorKeys.NAME_RSI_CURTO, IndicadorKeys.NAME_RSI_MEDIO, IndicadorKeys.NAME_RSI_LONGO,
                IndicadorKeys.NAME_RSI_STOCH_K, IndicadorKeys.NAME_RSI_STOCH_D
        ).forEach(k -> indicadoresMap.put(k, new ArrayList<>()));

        for (Analise a : analises) {
            final var time = a.getDataAnalise().atZone(ZoneId.systemDefault()).toEpochSecond();

            addPoint(indicadoresMap.get(IndicadorKeys.NAME_EMA), time, a.getEma());
            addPoint(indicadoresMap.get(IndicadorKeys.NAME_SMA), time, a.getSma());
            addPoint(indicadoresMap.get(IndicadorKeys.NAME_RSI_CURTO), time, a.getRsiCurto());
            addPoint(indicadoresMap.get(IndicadorKeys.NAME_RSI_MEDIO), time, a.getRsiMedio());
            addPoint(indicadoresMap.get(IndicadorKeys.NAME_RSI_LONGO), time, a.getRsiLongo());
            addPoint(indicadoresMap.get(IndicadorKeys.NAME_RSI_STOCH_K), time, a.getRsiEstocasticoK());
            addPoint(indicadoresMap.get(IndicadorKeys.NAME_RSI_STOCH_D), time, a.getRsiEstocasticoD());
        }

        final var eventos = buscarEventos(operacaoId, dataMin, dataMax);
        final var nomeEstrategia = operacao.getEstrategia() != null ? operacao.getEstrategia().getNome() : "Sem Estratégia";

        return new MonitoramentoDataDTO(operacao.getPar(), operacao.getIntervalo(), nomeEstrategia,
                calcularLucroTotal(operacaoId), candleChartDTOS, eventos, indicadoresMap);
    }

    private Map<String, Integer> extrairParametrosEstrategia(Set<IndicadorConfig> configs) {
        final var params = new HashMap<String, Integer>();

        if (configs == null)
            return params;

        for (IndicadorConfig config : configs) {
            var parametrosMap = new HashMap<String, Object>();

            try {
                parametrosMap = objectMapper.readValue(config.getParametros(), new TypeReference<>() {
                });
            } catch (JsonProcessingException e) {
                continue;
            }

            final var tipo = config.getTipoIndicador();

            if (tipo == TipoIndicador.RSI_CURTO) {
                params.put(IndicadorKeys.KEY_RSI_CURTO, getInt(parametrosMap, IndicadorKeys.PARAM_PERIODO_RSI_CURTO));
            } else if (tipo == TipoIndicador.RSI_MEDIO) {
                params.put(IndicadorKeys.KEY_RSI_MEDIO, getInt(parametrosMap, IndicadorKeys.PARAM_PERIODO_RSI_MEDIO));
            } else if (tipo == TipoIndicador.RSI_LONGO) {
                params.put(IndicadorKeys.KEY_RSI_LONGO, getInt(parametrosMap, IndicadorKeys.PARAM_PERIODO_RSI_LONGO));
            } else if (tipo == TipoIndicador.RSI_ESTOCASTICO_K || tipo == TipoIndicador.RSI_ESTOCASTICO_D) {
                params.put(IndicadorKeys.KEY_RSI_STOCH, getInt(parametrosMap, IndicadorKeys.PARAM_PERIODO_RSI_ESTOCASTICO));
                params.put(IndicadorKeys.KEY_STOCH_K, getInt(parametrosMap, IndicadorKeys.PARAM_SUAVIZACAO_K));
                params.put(IndicadorKeys.KEY_STOCH_D, getInt(parametrosMap, IndicadorKeys.PARAM_SUAVIZACAO_D));
            } else if (tipo == TipoIndicador.EMA) {
                params.put(IndicadorKeys.KEY_EMA, getInt(parametrosMap, IndicadorKeys.PARAM_PERIODO_EMA));
            } else if (tipo == TipoIndicador.SMA) {
                params.put(IndicadorKeys.KEY_SMA, getInt(parametrosMap, IndicadorKeys.PARAM_PERIODO_SMA));
            }
        }
        return params;
    }

    private Integer getInt(Map<String, Object> map, String key) {
        final var val = map.get(key);

        if (val instanceof Number)
            return ((Number) val).intValue();

        return 0;
    }

    private List<EventoChartDTO> buscarEventos(String operacaoId, Instant min, Instant max) {
        if (min == null || max == null)
            return Collections.emptyList();

        final var compras = compraRepository.findAllByOperacaoIdOrderByDataCriacaoDesc(operacaoId);
        final var eventos = new ArrayList<>(compras.stream()
                .filter(c -> isBetween(c.getDataCompra(), min, max))
                .map(c -> {
                    final var time = (c.getDataCandle() != null ? c.getDataCandle() : c.getDataCompra()).atZone(ZoneId.systemDefault()).toEpochSecond();
                    final var tipo = "COMPRA";
                    final var preco = c.getValorMoeda();
                    final var cor = "#26a69a";
                    final var tooltip = "Compra: " + c.getVolume();

                    return new EventoChartDTO(time, tipo, preco, cor, tooltip);
                })
                .toList());

        final var vendas = vendaRepository.findAllByOperacaoIdOrderByDataCriacaoDesc(operacaoId);
        eventos.addAll(vendas.stream()
                .filter(v -> isBetween(v.getDataVenda(), min, max))
                .map(v -> {
                    final var time = (v.getDataCandle() != null ? v.getDataCandle() : v.getDataVenda()).atZone(ZoneId.systemDefault()).toEpochSecond();
                    final var tipo = "VENDA";
                    final var preco = v.getValorVenda();
                    final var cor = "#ef5350";
                    final var tooltip = "Lucro: " + v.getLucro() + "%";

                    return new EventoChartDTO(time, tipo, preco, cor, tooltip);
                })
                .toList());

        return eventos;
    }

    private boolean isBetween(Instant date, Instant min, Instant max) {
        if (date == null)
            return false;

        return !date.isBefore(min) && !date.isAfter(max);
    }

    private void addPoint(List<IndicadorPointDTO> list, long time, BigDecimal value) {
        if (value != null)
            list.add(new IndicadorPointDTO(time, value));
    }

    private MonitoramentoDataDTO buildEmptyDTO(Operacao op) {
        final var nomeEstrategia = op.getEstrategia() != null ? op.getEstrategia().getNome() : "Sem Estratégia";

        return new MonitoramentoDataDTO(op.getPar(), op.getIntervalo(), nomeEstrategia, calcularLucroTotal(op.getId()),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyMap());
    }

    private BigDecimal calcularLucroTotal(String operacaoId) {
        return vendaRepository.findAllByOperacaoIdOrderByDataCriacaoDesc(operacaoId)
                .stream()
                .map(Venda::getLucro)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}