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
@Table(name = "vendas")
@EqualsAndHashCode(callSuper = true)
public class Venda extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_operacao", nullable = false)
    Operacao operacao;

    @Column(name = "data_venda", nullable = false)
    LocalDateTime dataVenda;

    @Column(name = "valor_compra", nullable = false)
    BigDecimal valorCompra;

    @Column(name = "valor_venda", nullable = false)
    BigDecimal valorVenda;

    @Column(name = "lucro", nullable = false)
    BigDecimal lucro;

    @Column(name = "snapshot_indicadores", columnDefinition = "TEXT")
    String snapshotIndicadores;

    @Override
    public void prePersist() {
        super.prePersist();
        if (this.operacao != null && this.operacao.getIdUsuario() != null) {
            this.setIdUsuario(this.operacao.getIdUsuario());
        }
    }
}