package br.com.bot_mexc.utils;

import br.com.bot_mexc.models.dtos.OperacaoDTO;
import br.com.bot_mexc.models.entities.Operacao;
import lombok.experimental.UtilityClass;

@UtilityClass
public class OperacoesUtils {

    public static OperacaoDTO converterEntidadeParaDto(Operacao operacao) {
        final var estrategiaDto = EstrategiaUtils.converterEntidadeParaDto(operacao.getEstrategia());
        final var operadorDto = OperadorUtils.converterEntidadeParaDto(operacao.getOperador());

        return OperacaoDTO.builder()
                .id(operacao.getId())
                .par(operacao.getPar())
                .intervalo(operacao.getIntervalo())
                .status(operacao.getStatus())
                .estrategia(estrategiaDto)
                .dataCriacao(operacao.getDataCriacao())
                .dataInicio(operacao.getDataInicio())
                .dataFim(operacao.getDataFim())
                .operador(operadorDto)
                .build();
    }

}