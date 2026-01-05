package br.com.bot_mexc.services;

import br.com.bot_mexc.models.dtos.CandleDTO;
import br.com.bot_mexc.models.dtos.monitoramento.CandleChartDTO;
import br.com.bot_mexc.models.dtos.monitoramento.EventoChartDTO;
import br.com.bot_mexc.models.dtos.monitoramento.IndicadorPointDTO;
import br.com.bot_mexc.models.dtos.monitoramento.MonitoramentoDataDTO;
import br.com.bot_mexc.models.entities.*;
import br.com.bot_mexc.models.enums.TipoIndicador;
import br.com.bot_mexc.repositories.AnaliseRepository;
import br.com.bot_mexc.repositories.CompraRepository;
import br.com.bot_mexc.repositories.OperacaoRepository;
import br.com.bot_mexc.repositories.VendaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    public MonitoramentoService(
            OperacaoRepository operacaoRepository,
            MexcService mexcService,
            AnaliseRepository analiseRepository,
            CompraRepository compraRepository,
            VendaRepository vendaRepository,
            ObjectMapper objectMapper
    ) {
        this.operacaoRepository = operacaoRepository;
        this.mexcService = mexcService;
        this.analiseRepository = analiseRepository;
        this.compraRepository = compraRepository;
        this.vendaRepository = vendaRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public MonitoramentoDataDTO buscarDadosMonitoramento(String operacaoId) {
        Operacao operacao = operacaoRepository.findById(operacaoId)
                .orElseThrow(() -> new RuntimeException("Operação não encontrada"));

        List<CandleDTO> candlesMexc = mexcService.consultarCandles(
                operacao.getPar(),
                operacao.getIntervalo(),
                "1000"
        );

        if (candlesMexc.isEmpty()) {
            return buildEmptyDTO(operacao);
        }

        List<CandleChartDTO> candleChartDTOS = new ArrayList<>();
        LocalDateTime dataMin = null;
        LocalDateTime dataMax = null;

        for (CandleDTO c : candlesMexc) {
            LocalDateTime dataCandle = c.dataAbertura();

            if (dataMin == null || dataCandle.isBefore(dataMin)) dataMin = dataCandle;
            if (dataMax == null || dataCandle.isAfter(dataMax)) dataMax = dataCandle;

            long timeSeconds = dataCandle.atZone(ZoneId.systemDefault()).toEpochSecond();

            candleChartDTOS.add(CandleChartDTO.builder()
                    .time(timeSeconds)
                    .open(c.valorAbertura())
                    .high(c.maxima())
                    .low(c.minima())
                    .close(c.valorFechamento())
                    .volume(c.volume())
                    .build());
        }

        Map<String, Integer> params = extrairParametrosEstrategia(operacao.getEstrategia().getIndicadoresConfig());

        List<Analise> analises = analiseRepository.buscarAnalisesCompativeis(
                operacao.getPar(),
                operacao.getIntervalo(),
                dataMin,
                dataMax,
                params.getOrDefault("RSI_CURTO", 0),
                params.getOrDefault("RSI_MEDIO", 0),
                params.getOrDefault("RSI_LONGO", 0),
                params.getOrDefault("RSI_STOCH", 0),
                params.getOrDefault("STOCH_K", 0),
                params.getOrDefault("STOCH_D", 0),
                params.get("EMA"),
                params.get("SMA")
        );

        Map<String, List<IndicadorPointDTO>> indicadoresMap = new HashMap<>();
        Arrays.asList("EMA", "SMA", "RSI_Curto", "RSI_Medio", "RSI_Longo", "RSI_Stoch_K", "RSI_Stoch_D")
                .forEach(k -> indicadoresMap.put(k, new ArrayList<>()));

        for (Analise a : analises) {
            long time = a.getDataAnalise().atZone(ZoneId.systemDefault()).toEpochSecond();
            addPoint(indicadoresMap.get("EMA"), time, a.getEma());
            addPoint(indicadoresMap.get("SMA"), time, a.getSma());
            addPoint(indicadoresMap.get("RSI_Curto"), time, a.getRsiCurto());
            addPoint(indicadoresMap.get("RSI_Medio"), time, a.getRsiMedio());
            addPoint(indicadoresMap.get("RSI_Longo"), time, a.getRsiLongo());
            addPoint(indicadoresMap.get("RSI_Stoch_K"), time, a.getRsiEstocasticoK());
            addPoint(indicadoresMap.get("RSI_Stoch_D"), time, a.getRsiEstocasticoD());
        }

        List<EventoChartDTO> eventos = buscarEventos(operacaoId, dataMin, dataMax);

        return MonitoramentoDataDTO.builder()
                .par(operacao.getPar())
                .intervalo(operacao.getIntervalo())
                .candles(candleChartDTOS)
                .indicadores(indicadoresMap)
                .eventos(eventos)
                .build();
    }

    private Map<String, Integer> extrairParametrosEstrategia(Set<IndicadorConfig> configs) {
        Map<String, Integer> params = new HashMap<>();

        if (configs == null) return params;

        for (IndicadorConfig config : configs) {
            Map<String, Object> parametrosMap;
            try {
                parametrosMap = objectMapper.readValue(config.getParametros(), new TypeReference<>() {
                });
            } catch (JsonProcessingException e) {
                continue;
            }

            TipoIndicador tipo = config.getTipoIndicador();

            // Mapeamento direto pelo Enum
            if (tipo == TipoIndicador.RSI_CURTO) {
                params.put("RSI_CURTO", getInt(parametrosMap, "periodo"));
            } else if (tipo == TipoIndicador.RSI_MEDIO) {
                params.put("RSI_MEDIO", getInt(parametrosMap, "periodo"));
            } else if (tipo == TipoIndicador.RSI_LONGO) {
                params.put("RSI_LONGO", getInt(parametrosMap, "periodo"));
            } else if (tipo == TipoIndicador.RSI_ESTOCASTICO_K || tipo == TipoIndicador.RSI_ESTOCASTICO_D) {
                params.put("RSI_STOCH", getInt(parametrosMap, "periodoRsi"));
                params.put("STOCH_K", getInt(parametrosMap, "suavizacaoK"));
                params.put("STOCH_D", getInt(parametrosMap, "suavizacaoD"));
            } else if (tipo == TipoIndicador.EMA) {
                params.put("EMA", getInt(parametrosMap, "periodo"));
            } else if (tipo == TipoIndicador.SMA) {
                params.put("SMA", getInt(parametrosMap, "periodo"));
            }
        }
        return params;
    }

    private Integer getInt(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return 0;
    }

    private List<EventoChartDTO> buscarEventos(String operacaoId, LocalDateTime min, LocalDateTime max) {
        if (min == null || max == null) return Collections.emptyList();

        List<Compra> compras = compraRepository.findAllByOperacaoIdOrderByDataCriacaoDesc(operacaoId);
        List<EventoChartDTO> eventos = new ArrayList<>(compras.stream()
                .filter(c -> isBetween(c.getDataCompra(), min, max))
                .map(c -> EventoChartDTO.builder()
                        .time(c.getDataCompra().atZone(ZoneId.systemDefault()).toEpochSecond())
                        .tipo("COMPRA")
                        .preco(c.getValorMoeda())
                        .cor("#26a69a")
                        .tooltip("Compra: " + c.getVolume())
                        .build())
                .toList());

        List<Venda> vendas = vendaRepository.findAllByOperacaoIdOrderByDataCriacaoDesc(operacaoId);
        eventos.addAll(vendas.stream()
                .filter(v -> isBetween(v.getDataVenda(), min, max))
                .map(v -> EventoChartDTO.builder()
                        .time(v.getDataVenda().atZone(ZoneId.systemDefault()).toEpochSecond())
                        .tipo("VENDA")
                        .preco(v.getValorVenda())
                        .cor("#ef5350")
                        .tooltip("Lucro: " + v.getLucro() + "%")
                        .build())
                .toList());

        return eventos;
    }

    private boolean isBetween(LocalDateTime date, LocalDateTime min, LocalDateTime max) {
        if (date == null) return false;
        return !date.isBefore(min) && !date.isAfter(max);
    }

    private void addPoint(List<IndicadorPointDTO> list, long time, BigDecimal value) {
        if (value != null) list.add(new IndicadorPointDTO(time, value));
    }

    private MonitoramentoDataDTO buildEmptyDTO(Operacao op) {
        return MonitoramentoDataDTO.builder()
                .par(op.getPar())
                .intervalo(op.getIntervalo())
                .candles(Collections.emptyList())
                .indicadores(Collections.emptyMap())
                .eventos(Collections.emptyList())
                .build();
    }
}