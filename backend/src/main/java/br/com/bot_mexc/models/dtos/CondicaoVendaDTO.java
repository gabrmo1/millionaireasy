package br.com.bot_mexc.models.dtos;

import br.com.bot_mexc.models.enums.OperadorLogico;
import br.com.bot_mexc.models.enums.PosicaoFaixasCompraVenda;
import br.com.bot_mexc.models.enums.TipoIndicador;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CondicaoVendaDTO(
        String id,
        String idEstrategia,
        TipoIndicador tipoIndicador,
        BigDecimal valorIndicador,
        PosicaoFaixasCompraVenda posicaoFaixa,
        Integer ordem,
        OperadorLogico operadorParaProxima
) {
}