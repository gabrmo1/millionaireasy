package br.com.bot_mexc.utils;

import br.com.bot_mexc.models.dtos.CondicaoCompraDTO;
import br.com.bot_mexc.models.entities.CondicaoCompra;
import br.com.bot_mexc.models.entities.Estrategia;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CondicaoCompraUtils {

    public static CondicaoCompra converterDtoParaEntidade(CondicaoCompraDTO dto, Estrategia estrategia) {
        return CondicaoCompra.builder()
                .estrategia(estrategia)
                .ordem(dto.ordem())
                .operadorParaProxima(dto.operadorParaProxima())
                .operandoATipo(dto.operandoATipo())
                .operandoAReferencia(dto.operandoAReferencia())
                .operandoAValor(dto.operandoAValor())
                .operador(dto.operador())
                .operandoBTipo(dto.operandoBTipo())
                .operandoBReferencia(dto.operandoBReferencia())
                .operandoBValor(dto.operandoBValor())
                .build();
    }

    public static CondicaoCompraDTO converterEntidadeParaDto(CondicaoCompra entidade) {
        return CondicaoCompraDTO.builder()
                .id(entidade.getId())
                .idEstrategia(entidade.getEstrategia().getId())
                .ordem(entidade.getOrdem())
                .operadorParaProxima(entidade.getOperadorParaProxima())
                .operandoATipo(entidade.getOperandoATipo())
                .operandoAReferencia(entidade.getOperandoAReferencia())
                .operandoAValor(entidade.getOperandoAValor())
                .operador(entidade.getOperador())
                .operandoBTipo(entidade.getOperandoBTipo())
                .operandoBReferencia(entidade.getOperandoBReferencia())
                .operandoBValor(entidade.getOperandoBValor())
                .build();
    }
}