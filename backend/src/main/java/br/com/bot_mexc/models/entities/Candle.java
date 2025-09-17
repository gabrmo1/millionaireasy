package br.com.bot_mexc.models.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "candles")
@EqualsAndHashCode(callSuper = true)
public class Candle extends BaseEntity {

    @Column(name = "par", length = 20, nullable = false)
    String par;

    @Column(name = "intervalo", nullable = false)
    String intervalo;

    @Column(name = "data_abertura", nullable = false)
    LocalDateTime dataAbertura;

    @Column(name = "data_fechamento", nullable = false)
    LocalDateTime dataFechamento;

    @Column(name = "valor_abertura", nullable = false)
    BigDecimal valorAbertura;

    @Column(name = "valor_fechamento", nullable = false)
    BigDecimal valorFechamento;

    @Column(name = "maxima", nullable = false)
    BigDecimal maxima;

    @Column(name = "minima", nullable = false)
    BigDecimal minima;

    @Column(name = "volume", nullable = false)
    BigDecimal volume;


}
