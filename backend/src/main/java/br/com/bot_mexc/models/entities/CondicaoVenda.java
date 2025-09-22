package br.com.bot_mexc.models.entities;

import br.com.bot_mexc.models.enums.OperadorLogico;
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
@Table(name = "condicoes_venda")
public class CondicaoVenda extends BaseEntity {

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

    @Column(name = "ordem", nullable = false)
    private Integer ordem;

    @Enumerated(EnumType.STRING)
    @Column(name = "operador_para_proxima")
    private OperadorLogico operadorParaProxima;

}