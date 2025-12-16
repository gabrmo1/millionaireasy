package br.com.bot_mexc.builders;

import br.com.bot_mexc.models.dtos.CriarOperacaoDTO;
import br.com.bot_mexc.models.entities.Estrategia;
import br.com.bot_mexc.models.entities.Operacao;
import br.com.bot_mexc.models.entities.Operador;
import br.com.bot_mexc.models.enums.StatusOperacoes;
import lombok.experimental.UtilityClass;

@UtilityClass
public class OperacaoBuilder {

    public static Operacao montarOperacao(CriarOperacaoDTO request, Operador operador, Estrategia estrategia) {
        return Operacao.builder()
                .estrategia(estrategia)
                .operador(operador)
                .status(StatusOperacoes.PARADO)
                .par(request.par())
                .intervalo(request.intervalo())
                .modoTeste(Boolean.TRUE.equals(request.modoTeste()))
                .saldoInicial(request.saldoInicial())
                .build();
    }

}