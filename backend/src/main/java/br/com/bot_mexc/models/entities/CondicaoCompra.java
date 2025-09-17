package br.com.bot_mexc.models.entities;

import br.com.bot_mexc.models.enums.PosicaoFaixasCompraVenda;
import br.com.bot_mexc.models.enums.TipoIndicador;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = "estrategia")
@ToString(callSuper = true, exclude = "estrategia")
@Table(name = "condicoes_compra")
public class CondicaoCompra extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estrategia", nullable = false)
    private Estrategia estrategia;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_indicador", nullable = false)
    private TipoIndicador tipoIndicador;

    @Column(name = "valor_indicador")
    private BigDecimal valorIndicador;

    @Enumerated(EnumType.STRING)
    @Column(name = "posicao_faixa")
    private PosicaoFaixasCompraVenda posicaoFaixa;

    @Column(name = "valor_operacao_fixo")
    private BigDecimal valorOperacaoFixo;

    @Column(name = "percentual_valor_operacao")
    private BigDecimal percentualValorOperacao;
}