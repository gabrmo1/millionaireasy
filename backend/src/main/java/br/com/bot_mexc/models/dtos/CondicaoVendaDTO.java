package br.com.bot_mexc.models.dtos;

import br.com.bot_mexc.models.enums.PosicaoFaixasCompraVenda;
import br.com.bot_mexc.models.enums.TipoIndicador;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CondicaoVendaDTO(
        String id,
        String idEstrategia,
        TipoIndicador tipoIndicador,
        BigDecimal valorIndicador,
        PosicaoFaixasCompraVenda posicaoFaixa,
        @Positive(message = "A quantia sobre o lucro deve ser um valor positivo.")
        @Max(value = 100, message = "A quantia sobre o lucro não pode ser maior que 100.")
        BigDecimal quantiaSobreLucro
) {
}