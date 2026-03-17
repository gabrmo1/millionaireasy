package br.com.bot_mexc.models.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "pausas_operacoes")
@EqualsAndHashCode(callSuper = true)
public class PausaOperacao extends BaseEntity { //TODO: remover

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_operacao", nullable = false)
    Operacao operacao;

    @Column(name = "data_pausa")
    LocalDateTime dataPausa;

}
