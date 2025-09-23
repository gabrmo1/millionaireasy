package br.com.bot_mexc.models.dtos;

import br.com.bot_mexc.models.enums.OperadorComparacao;
import br.com.bot_mexc.models.enums.OperadorLogico;
import br.com.bot_mexc.models.enums.TipoOperando;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CondicaoVendaDTO(
        String id,
        String idEstrategia,
        Integer ordem,
        OperadorLogico operadorParaProxima,
        TipoOperando operandoATipo,
        String operandoAReferencia,
        BigDecimal operandoAValor,
        OperadorComparacao operador,
        TipoOperando operandoBTipo,
        String operandoBReferencia,
        BigDecimal operandoBValor
) {}