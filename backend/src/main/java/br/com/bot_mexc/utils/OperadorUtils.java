package br.com.bot_mexc.utils;

import br.com.bot_mexc.models.dtos.CriarOperadorDTO;
import br.com.bot_mexc.models.dtos.OperadorDTO;
import br.com.bot_mexc.models.entities.Operador;
import lombok.experimental.UtilityClass;

@UtilityClass
public class OperadorUtils {

    public static OperadorDTO converterEntidadeParaDto(Operador operador) {
        return OperadorDTO.builder()
                .id(operador.getId())
                .nome(operador.getNome())
                .accessKey(operador.getAccessKey())
                .secretKey(operador.getSecretKey())
                .build();
    }

    public static Operador converterDtoParaEntidade(CriarOperadorDTO request) {
        var novoOperador = new Operador();
        atualizarEntidadeComDto(novoOperador, request);
        return novoOperador;
    }

    public static void atualizarEntidadeComDto(Operador operador, CriarOperadorDTO dto) {
        operador.setNome(dto.nome());
        operador.setAccessKey(dto.accessKey());
        operador.setSecretKey(dto.secretKey());
    }

}