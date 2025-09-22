package br.com.bot_mexc.models.dtos;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record EstrategiaDTO(
        String id,
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
        BigDecimal valorOperacaoFixo,
        String stablecoin,
        BigDecimal percentualValorOperacao,
        Boolean vendaApenasPorLucro,
        BigDecimal percentualLucro,
        List<CondicaoCompraDTO> condicoesCompra,
        List<CondicaoVendaDTO> condicoesVenda
) {
}