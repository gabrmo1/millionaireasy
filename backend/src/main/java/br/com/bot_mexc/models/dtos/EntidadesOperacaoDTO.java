package br.com.bot_mexc.models.dtos;

import br.com.bot_mexc.models.entities.Estrategia;
import br.com.bot_mexc.models.entities.Operador;

public record EntidadesOperacaoDTO(

        Operador operador,

        Estrategia estrategia

) {
}
