// backend/src/main/java/br/com/bot_mexc/utils/EstrategiaUtils.java
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

import br.com.bot_mexc.modules.strategy.dtos.CriarEstrategiaDTO;
import br.com.bot_mexc.modules.strategy.dtos.EstrategiaDTO;
import br.com.bot_mexc.modules.strategy.entities.Estrategia;
import lombok.experimental.UtilityClass;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.stream.Collectors;

@UtilityClass
public class EstrategiaUtils {

    public static Estrategia converterDtoParaEntidade(CriarEstrategiaDTO dto) {
        Estrategia entidade = new Estrategia();
        atualizarEntidadeComDto(entidade, dto);
        return entidade;
    }

    public static void atualizarEntidadeComDto(Estrategia entidade, CriarEstrategiaDTO dto) {
        entidade.setNome(dto.nome());
        entidade.setValorOperacaoFixo(dto.valorOperacaoFixo());
        entidade.setStablecoin(dto.stablecoin());
        entidade.setPercentualValorOperacao(dto.percentualValorOperacao());
        entidade.setVendaApenasPorLucro(dto.vendaApenasPorLucro());
        entidade.setPercentualLucro(dto.percentualLucro());
    }

    public static EstrategiaDTO converterEntidadeParaDto(Estrategia entidade) {
        if (entidade == null) {
            return null;
        }

        return EstrategiaDTO.builder()
                .id(entidade.getId())
                .nome(entidade.getNome())
                .valorOperacaoFixo(entidade.getValorOperacaoFixo())
                .stablecoin(entidade.getStablecoin())
                .percentualValorOperacao(entidade.getPercentualValorOperacao())
                .vendaApenasPorLucro(entidade.getVendaApenasPorLucro())
                .percentualLucro(entidade.getPercentualLucro())
                .indicadoresConfig(
                        !CollectionUtils.isEmpty(entidade.getIndicadoresConfig()) ?
                                entidade.getIndicadoresConfig().stream().map(IndicadorConfigUtils::converterEntidadeParaDto).collect(Collectors.toList()) :
                                Collections.emptyList()
                )
                .condicoesCompra(
                        !CollectionUtils.isEmpty(entidade.getCondicoesCompra()) ?
                                entidade.getCondicoesCompra().stream().map(CondicaoCompraUtils::converterEntidadeParaDto).collect(Collectors.toList()) :
                                Collections.emptyList()
                )
                .condicoesVenda(
                        !CollectionUtils.isEmpty(entidade.getCondicoesVenda()) ?
                                entidade.getCondicoesVenda().stream().map(CondicaoVendaUtils::converterEntidadeParaDto).collect(Collectors.toList()) :
                                Collections.emptyList()
                )
                .build();
    }
}