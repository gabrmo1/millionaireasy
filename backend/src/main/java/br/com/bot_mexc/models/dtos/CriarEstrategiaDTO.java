package br.com.bot_mexc.models.dtos;

import br.com.bot_mexc.models.enums.TipoIndicador;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotBlank;
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
        Boolean utilizarRsiCurto,
        Integer periodoRsiCurto,
        Boolean utilizarRsiMedio,
        Integer periodoRsiMedio,
        Boolean utilizarRsiLongo,
        Integer periodoRsiLongo,
        Boolean utilizarRsiEstocastico,
        Integer periodoRsiEstocastico,
        Integer suavizacaoRsiEstocasticoD,
        Integer suavizacaoRsiEstocasticoK,
        Boolean utilizarEma,
        Integer periodoEma,
        Boolean utilizarSma,
        Integer periodoSma,
        Boolean realizarLeituraVolume,
        @Positive(message = "O valor fixo da operação deve ser positivo.")
        BigDecimal valorOperacaoFixo,
        String stablecoin,
        @Positive(message = "O percentual da operação deve ser positivo.")
        BigDecimal percentualValorOperacao,
        Boolean vendaApenasPorLucro,
        BigDecimal percentualLucro,
        List<CondicaoCompraDTO> condicoesCompra,
        List<CondicaoVendaDTO> condicoesVenda
) {
    public CriarEstrategiaDTO {
        boolean hasValor = (Objects.nonNull(valorOperacaoFixo) && valorOperacaoFixo.compareTo(BigDecimal.ZERO) > 0) ||
                (Objects.nonNull(percentualValorOperacao) && percentualValorOperacao.compareTo(BigDecimal.ZERO) > 0);

        if (hasValor && (Objects.isNull(stablecoin) || stablecoin.isBlank())) {
            throw new ValidationException("A Stablecoin é obrigatória quando um valor de operação (fixo ou percentual) é definido.");
        }

        if (!CollectionUtils.isEmpty(condicoesCompra)) {
            if (Objects.isNull(valorOperacaoFixo) && Objects.isNull(percentualValorOperacao)) {
                throw new ValidationException("Os parâmetros de compra (Valor Fixo ou Percentual) são obrigatórios quando existem condições de compra.");
            }
        }

        if (!CollectionUtils.isEmpty(condicoesCompra)) {
            for (var condicao : condicoesCompra) {
                if (!isIndicatorEnabled(condicao.tipoIndicador(), utilizarRsiCurto, utilizarRsiMedio, utilizarRsiLongo, utilizarRsiEstocastico, utilizarEma, utilizarSma, realizarLeituraVolume)) {
                    throw new ValidationException("A condição de compra para o indicador " + condicao.tipoIndicador() + " é inválida, pois o indicador não está habilitado na estratégia.");
                }
                if (isRsiIndicator(condicao.tipoIndicador())) {
                    validarValorRsi(condicao.valorIndicador());
                }
            }
        }

        if (!CollectionUtils.isEmpty(condicoesVenda)) {
            for (var condicao : condicoesVenda) {
                if (!isIndicatorEnabled(condicao.tipoIndicador(), utilizarRsiCurto, utilizarRsiMedio, utilizarRsiLongo, utilizarRsiEstocastico, utilizarEma, utilizarSma, realizarLeituraVolume)) {
                    throw new ValidationException("A condição de venda para o indicador " + condicao.tipoIndicador() + " é inválida, pois o indicador não está habilitado na estratégia.");
                }
                if (isRsiIndicator(condicao.tipoIndicador())) {
                    validarValorRsi(condicao.valorIndicador());
                }
            }
        }

        if (Boolean.TRUE.equals(vendaApenasPorLucro)) {
            if (Objects.isNull(percentualLucro)) {
                throw new ValidationException("O percentual de lucro é obrigatório quando a venda por lucro está ativada.");
            }
            if (percentualLucro.compareTo(BigDecimal.ZERO) < 0 || percentualLucro.compareTo(new BigDecimal("9999")) > 0) {
                throw new ValidationException("O percentual de lucro deve estar entre 0 e 9999.");
            }
        }
    }

    private static boolean isRsiIndicator(TipoIndicador indicator) {
        return indicator == TipoIndicador.RSI_CURTO ||
                indicator == TipoIndicador.RSI_MEDIO ||
                indicator == TipoIndicador.RSI_LONGO ||
                indicator == TipoIndicador.RSI_ESTOCASTICO_K ||
                indicator == TipoIndicador.RSI_ESTOCASTICO_D;
    }

    private static void validarValorRsi(BigDecimal valor) {
        if (Objects.isNull(valor)) {
            throw new ValidationException("O valor do indicador RSI não pode ser nulo.");
        }
        if (valor.compareTo(BigDecimal.ZERO) < 0 || valor.compareTo(new BigDecimal("100")) > 0) {
            throw new ValidationException("O valor para indicadores RSI deve estar entre 0 e 100.");
        }
    }

    private static boolean isIndicatorEnabled(TipoIndicador indicator, Boolean utilizarRsiCurto, Boolean utilizarRsiMedio, Boolean utilizarRsiLongo, Boolean utilizarRsiEstocastico, Boolean utilizarEma, Boolean utilizarSma, Boolean realizarLeituraVolume) {
        return switch (indicator) {
            case RSI_CURTO -> utilizarRsiCurto;
            case RSI_MEDIO -> utilizarRsiMedio;
            case RSI_LONGO -> utilizarRsiLongo;
            case RSI_ESTOCASTICO_K, RSI_ESTOCASTICO_D -> utilizarRsiEstocastico;
            case EMA -> utilizarEma;
            case SMA -> utilizarSma;
            case VOLUME -> realizarLeituraVolume;
        };
    }
}