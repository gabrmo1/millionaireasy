package br.com.bot_mexc.models.dtos;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record EstrategiaDTO(
        String id,
        String nome,
        BigDecimal valorOperacaoFixo,
        String stablecoin,
        BigDecimal percentualValorOperacao,
        Boolean vendaApenasPorLucro,
        BigDecimal percentualLucro,
        List<IndicadorConfigDTO> indicadoresConfig,
        List<CondicaoCompraDTO> condicoesCompra,
        List<CondicaoVendaDTO> condicoesVenda
) {}