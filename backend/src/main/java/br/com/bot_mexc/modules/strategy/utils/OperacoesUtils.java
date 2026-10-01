package br.com.bot_mexc.modules.strategy.utils;
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

import br.com.bot_mexc.modules.strategy.dtos.CriarOperacaoDTO;
import br.com.bot_mexc.modules.strategy.dtos.OperacaoDTO;
import br.com.bot_mexc.modules.strategy.entities.Estrategia;
import br.com.bot_mexc.modules.strategy.entities.Operacao;
import br.com.bot_mexc.modules.strategy.entities.Operador;
import lombok.experimental.UtilityClass;

@UtilityClass
public class OperacoesUtils {

    public static OperacaoDTO converterEntidadeParaDto(Operacao operacao) {
        final var estrategiaDto = EstrategiaUtils.converterEntidadeParaDto(operacao.getEstrategia());
        final var operadorDto = operacao.getOperador() != null
                ? OperadorUtils.converterEntidadeParaDto(operacao.getOperador())
                : null;

        return OperacaoDTO.builder()
                .id(operacao.getId())
                .par(operacao.getPar())
                .intervalo(operacao.getIntervalo())
                .status(operacao.getStatus())
                .estrategia(estrategiaDto)
                .dataCriacao(operacao.getDataCriacao())
                .dataInicio(operacao.getDataInicio())
                .dataFim(operacao.getDataFim())
                .operador(operadorDto)
                .modoTeste(operacao.getModoTeste())
                .saldoInicial(operacao.getSaldoInicial())
                .build();
    }

    public static void atualizarEntidadeComDto(Operacao operacao, CriarOperacaoDTO dto, Operador operador, Estrategia estrategia) {
        operacao.setPar(dto.par());
        operacao.setIntervalo(dto.intervalo());
        operacao.setOperador(operador);
        operacao.setEstrategia(estrategia);
    }

}