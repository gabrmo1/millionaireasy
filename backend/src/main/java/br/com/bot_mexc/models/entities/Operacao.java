package br.com.bot_mexc.models.entities;

import br.com.bot_mexc.models.enums.StatusOperacoes;
import br.com.bot_mexc.models.enums.TipoOperacao;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "operacoes")
@EqualsAndHashCode(callSuper = true)
public class Operacao extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 12, nullable = false)
    StatusOperacoes status;

    @Column(name = "data_inicio")
    Instant dataInicio;

    @Column(name = "data_fim")
    Instant dataFim;

    @Column(name = "par", length = 20, nullable = false)
    String par;

    @Column(name = "intervalo", nullable = false, length = 5)
    String intervalo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_operador")
    Operador operador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estrategia")
    Estrategia estrategia;

    @Column(name = "modo_teste", nullable = false)
    @Builder.Default
    Boolean modoTeste = false;

    @Column(name = "saldo_inicial")
    BigDecimal saldoInicial;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacao", length = 20, nullable = false)
    @Builder.Default
    TipoOperacao tipoOperacao = TipoOperacao.LIVE;
}