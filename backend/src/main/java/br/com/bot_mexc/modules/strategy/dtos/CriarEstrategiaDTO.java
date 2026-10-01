package br.com.bot_mexc.modules.strategy.dtos;
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

import br.com.bot_mexc.shared.enums.TipoOperando;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Builder
public record CriarEstrategiaDTO(
        String id,
        @NotBlank(message = "O nome da estratégia é obrigatório.")
        String nome,
        @Positive(message = "O valor fixo da operação deve ser positivo.")
        BigDecimal valorOperacaoFixo,
        @NotBlank(message = "A Stablecoin é obrigatória.")
        String stablecoin,
        @Positive(message = "O percentual da operação deve ser positivo.")
        BigDecimal percentualValorOperacao,
        Boolean vendaApenasPorLucro,
        BigDecimal percentualLucro,
        @NotNull @Valid
        List<IndicadorConfigDTO> indicadoresConfig,
        @Valid
        List<CondicaoCompraDTO> condicoesCompra,
        @Valid
        List<CondicaoVendaDTO> condicoesVenda
) {
    public CriarEstrategiaDTO {
        if (Boolean.TRUE.equals(vendaApenasPorLucro)) {
            if (Objects.isNull(percentualLucro) || percentualLucro.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("O percentual de lucro é obrigatório e deve ser positivo quando a venda por lucro está ativada.");
            }
        }

        // --- NOVA VALIDAÇÃO ADICIONADA ---
        if (!CollectionUtils.isEmpty(condicoesCompra)) {
            for (CondicaoCompraDTO cond : condicoesCompra) {
                if (cond.operandoATipo() == TipoOperando.VALOR_FIXO && cond.operandoAValor() == null) {
                    throw new ValidationException("O campo 'Valor Fixo' do Operando A é obrigatório na condição de compra #" + (cond.ordem() + 1));
                }
                if (cond.operandoBTipo() == TipoOperando.VALOR_FIXO && cond.operandoBValor() == null) {
                    throw new ValidationException("O campo 'Valor Fixo' do Operando B é obrigatório na condição de compra #" + (cond.ordem() + 1));
                }
            }
        }

        if (!CollectionUtils.isEmpty(condicoesVenda)) {
            for (CondicaoVendaDTO cond : condicoesVenda) {
                if (cond.operandoATipo() == TipoOperando.VALOR_FIXO && cond.operandoAValor() == null) {
                    throw new ValidationException("O campo 'Valor Fixo' do Operando A é obrigatório na condição de venda #" + (cond.ordem() + 1));
                }
                if (cond.operandoBTipo() == TipoOperando.VALOR_FIXO && cond.operandoBValor() == null) {
                    throw new ValidationException("O campo 'Valor Fixo' do Operando B é obrigatório na condição de venda #" + (cond.ordem() + 1));
                }
            }
        }
    }
}