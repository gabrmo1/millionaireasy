package br.com.bot_mexc.models.dtos;

import java.util.List;

public record HistoricoOperacaoDTO(
        List<CompraDTO> compras,
        List<VendaDTO> vendas
) {
}