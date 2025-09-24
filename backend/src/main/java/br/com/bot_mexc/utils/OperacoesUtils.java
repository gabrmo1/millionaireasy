package br.com.bot_mexc.utils;

import br.com.bot_mexc.models.dtos.CriarOperacaoDTO;
import br.com.bot_mexc.models.dtos.OperacaoDTO;
import br.com.bot_mexc.models.entities.Estrategia;
import br.com.bot_mexc.models.entities.Operacao;
import br.com.bot_mexc.models.entities.Operador;
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

    public static void atualizarEntidadeComDto(Operacao operacao, CriarOperacaoDTO dto, Operador operador, Estrategia estrategia) {
        operacao.setPar(dto.par());
        operacao.setIntervalo(dto.intervalo());
        operacao.setOperador(operador);
        operacao.setEstrategia(estrategia);
    }

}