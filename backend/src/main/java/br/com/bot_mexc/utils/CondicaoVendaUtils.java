package br.com.bot_mexc.utils;

import br.com.bot_mexc.models.dtos.CondicaoVendaDTO;
import br.com.bot_mexc.models.entities.CondicaoVenda;
import br.com.bot_mexc.models.entities.Estrategia;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CondicaoVendaUtils {

    public static CondicaoVenda converterDtoParaEntidade(CondicaoVendaDTO dto, Estrategia estrategia) {
        return CondicaoVenda.builder()
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

    public static CondicaoVendaDTO converterEntidadeParaDto(CondicaoVenda entidade) {
        return CondicaoVendaDTO.builder()
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