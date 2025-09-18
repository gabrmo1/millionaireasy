package br.com.bot_mexc.models.dtos;

import br.com.bot_mexc.models.enums.TipoIndicador;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record EstrategiaDTO(
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
        @Positive(message = "O percentual da operação deve ser positivo.")
        BigDecimal percentualValorOperacao,
        List<CondicaoCompraDTO> condicoesCompra,
        List<CondicaoVendaDTO> condicoesVenda
) {
    public EstrategiaDTO {
        if (!CollectionUtils.isEmpty(condicoesCompra)) {
            for (var condicao : condicoesCompra) {
                if (!isIndicatorEnabled(condicao.tipoIndicador(), utilizarRsiCurto, utilizarRsiMedio, utilizarRsiLongo, utilizarRsiEstocastico, utilizarEma, utilizarSma, realizarLeituraVolume)) {
                    throw new ValidationException("A condição de compra para o indicador " + condicao.tipoIndicador() + " é inválida, pois o indicador não está habilitado na estratégia.");
                }
            }
        }

        if (!CollectionUtils.isEmpty(condicoesVenda)) {
            for (var condicao : condicoesVenda) {
                if (!isIndicatorEnabled(condicao.tipoIndicador(), utilizarRsiCurto, utilizarRsiMedio, utilizarRsiLongo, utilizarRsiEstocastico, utilizarEma, utilizarSma, realizarLeituraVolume)) {
                    throw new ValidationException("A condição de venda para o indicador " + condicao.tipoIndicador() + " é inválida, pois o indicador não está habilitado na estratégia.");
                }
            }
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