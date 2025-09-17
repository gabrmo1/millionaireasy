package br.com.bot_mexc.models.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "compras")
@EqualsAndHashCode(callSuper = true)
public class Compra extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_operacao", nullable = false)
    Operacao operacao;

    @Column(name = "data_compra", nullable = false)
    LocalDateTime data_compra;

    @Column(name = "valor_operacao", nullable = false)
    BigDecimal valor_operacao;

    @Column(name = "valor_moeda", nullable = false)
    BigDecimal valor_moeda;

    @Column(name = "volume", nullable = false)
    BigDecimal volume;

}
