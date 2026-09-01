package br.com.bot_mexc.models.dtos.monitoramento;

import br.com.bot_mexc.models.entities.Compra;
import br.com.bot_mexc.models.entities.Venda;

/**
 * Record auxiliar para pareamento lógico em memória (Mechanical Sympathy).
 * Mantém referências imutáveis das entidades para evitar novas alocações no loop de cálculo.
 */
public record TradeMatchDTO(
        Compra compra,
        Venda venda
) {
    public boolean isVencedor() {
        return venda != null && venda.getLucro().compareTo(java.math.BigDecimal.ZERO) > 0;
    }

    public boolean isPerdedor() {
        return venda != null && venda.getLucro().compareTo(java.math.BigDecimal.ZERO) <= 0;
    }
}