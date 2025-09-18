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
                .tipoIndicador(dto.tipoIndicador())
                .valorIndicador(dto.valorIndicador())
                .posicaoFaixa(dto.posicaoFaixa())
                .build();
    }

    public static CondicaoVendaDTO converterEntidadeParaDto(CondicaoVenda entidade) {
        return CondicaoVendaDTO.builder()
                .id(entidade.getId())
                .idEstrategia(entidade.getEstrategia().getId())
                .tipoIndicador(entidade.getTipoIndicador())
                .valorIndicador(entidade.getValorIndicador())
                .posicaoFaixa(entidade.getPosicaoFaixa())
                .build();
    }
}