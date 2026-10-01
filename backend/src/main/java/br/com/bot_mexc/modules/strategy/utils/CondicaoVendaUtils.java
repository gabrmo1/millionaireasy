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

import br.com.bot_mexc.modules.strategy.dtos.CondicaoVendaDTO;
import br.com.bot_mexc.modules.strategy.entities.CondicaoVenda;
import br.com.bot_mexc.modules.strategy.entities.Estrategia;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CondicaoVendaUtils {

    public static CondicaoVenda converterDtoParaEntidade(CondicaoVendaDTO dto, Estrategia estrategia) {
        return CondicaoVenda.builder()
                .estrategia(estrategia)
                .ordem(dto.ordem())
                .operadorParaProxima(dto.operadorParaProxima())
                .operandoATipo(dto.operandoATipo())
                .operandoAReferencia(dto.operandoAReferencia())
                .operandoAValor(dto.operandoAValor())
                .operador(dto.operador())
                .operandoBTipo(dto.operandoBTipo())
                .operandoBReferencia(dto.operandoBReferencia())
                .operandoBValor(dto.operandoBValor())
                .build();
    }

    public static CondicaoVendaDTO converterEntidadeParaDto(CondicaoVenda entidade) {
        return CondicaoVendaDTO.builder()
                .id(entidade.getId())
                .idEstrategia(entidade.getEstrategia().getId())
                .ordem(entidade.getOrdem())
                .operadorParaProxima(entidade.getOperadorParaProxima())
                .operandoATipo(entidade.getOperandoATipo())
                .operandoAReferencia(entidade.getOperandoAReferencia())
                .operandoAValor(entidade.getOperandoAValor())
                .operador(entidade.getOperador())
                .operandoBTipo(entidade.getOperandoBTipo())
                .operandoBReferencia(entidade.getOperandoBReferencia())
                .operandoBValor(entidade.getOperandoBValor())
                .build();
    }
}