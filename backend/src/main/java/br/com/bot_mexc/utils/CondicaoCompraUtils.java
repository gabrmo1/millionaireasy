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
                .tipoIndicador(dto.tipoIndicador())
                .valorIndicador(dto.valorIndicador())
                .posicaoFaixa(dto.posicaoFaixa())
                .build();
    }

    public static CondicaoCompraDTO converterEntidadeParaDto(CondicaoCompra entidade) {
        return CondicaoCompraDTO.builder()
                .id(entidade.getId())
                .idEstrategia(entidade.getEstrategia().getId())
                .tipoIndicador(entidade.getTipoIndicador())
                .valorIndicador(entidade.getValorIndicador())
                .posicaoFaixa(entidade.getPosicaoFaixa())
                .build();
    }
}