package br.com.bot_mexc.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

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
}