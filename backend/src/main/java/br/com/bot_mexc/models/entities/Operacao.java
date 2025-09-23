package br.com.bot_mexc.models.entities;

import br.com.bot_mexc.models.enums.StatusOperacoes;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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
    LocalDateTime dataInicio;

    @Column(name = "data_fim")
    LocalDateTime dataFim;

    @Column(name = "par", length = 20, nullable = false)
    String par;

    @Column(name = "intervalo", nullable = false, length = 5)
    String intervalo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_operador", nullable = false)
    Operador operador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estrategia")
    Estrategia estrategia;

}