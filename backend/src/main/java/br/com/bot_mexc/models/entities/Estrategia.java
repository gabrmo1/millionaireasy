package br.com.bot_mexc.models.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Set;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = {"condicoesCompra", "condicoesVenda", "indicadoresConfig"})
@ToString(callSuper = true, exclude = {"condicoesCompra", "condicoesVenda", "indicadoresConfig"})
@Table(name = "estrategias")
public class Estrategia extends BaseEntity {

    @Column(name = "nome", nullable = false, length = 50)
    private String nome;

    /*---------- Relações ----------*/
    @OneToMany(mappedBy = "estrategia", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<IndicadorConfig> indicadoresConfig;

    @OrderBy("ordem ASC")
    @OneToMany(mappedBy = "estrategia", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<CondicaoCompra> condicoesCompra;

    @OrderBy("ordem ASC")
    @OneToMany(mappedBy = "estrategia", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<CondicaoVenda> condicoesVenda;

    /*---------- Valores de Operação ----------*/
    @Column(name = "valor_operacao_fixo")
    private BigDecimal valorOperacaoFixo;

    @Column(name = "stablecoin", length = 10)
    private String stablecoin;

    @Column(name = "percentual_valor_operacao")
    private BigDecimal percentualValorOperacao;

    /*---------- Venda por Lucro ----------*/
    @Column(name = "venda_apenas_por_lucro")
    private Boolean vendaApenasPorLucro;

    @Column(name = "percentual_lucro")
    private BigDecimal percentualLucro;
}