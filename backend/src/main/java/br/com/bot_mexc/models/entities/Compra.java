package br.com.bot_mexc.models.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "compras")
@EqualsAndHashCode(callSuper = true)
public class Compra extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_operacao", nullable = false)
    Operacao operacao;

    @Column(name = "data_compra", nullable = false)
    Instant dataCompra;

    @Column(name = "data_candle")
    Instant dataCandle;

    @Column(name = "valor_operacao", nullable = false)
    BigDecimal valorOperacao;

    @Column(name = "valor_moeda", nullable = false)
    BigDecimal valorMoeda;

    @Column(name = "volume", nullable = false)
    BigDecimal volume;

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