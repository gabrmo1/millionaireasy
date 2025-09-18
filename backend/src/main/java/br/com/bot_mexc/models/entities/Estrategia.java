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
@EqualsAndHashCode(callSuper = true, exclude = {"condicoesCompra", "condicoesVenda"})
@ToString(callSuper = true, exclude = {"condicoesCompra", "condicoesVenda"})
@Table(name = "estrategias")
public class Estrategia extends BaseEntity {

    @Column(name = "nome", nullable = false, length = 50)
    String nome;

    @OneToMany(mappedBy = "estrategia", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<CondicaoCompra> condicoesCompra;

    @OneToMany(mappedBy = "estrategia", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<CondicaoVenda> condicoesVenda;

    /*---------- Valores de Operação ----------*/
    @Column(name = "valor_operacao_fixo")
    private BigDecimal valorOperacaoFixo;

    @Column(name = "percentual_valor_operacao")
    private BigDecimal percentualValorOperacao;

    /*---------- RSI curto ----------*/
    @Column(name = "utilizar_rsi_curto", nullable = false)
    Boolean utilizarRsiCurto;

    @Column(name = "periodo_rsi_curto")
    Integer periodoRsiCurto;

    /*---------- RSI médio ----------*/
    @Column(name = "utilizar_rsi_medio", nullable = false)
    Boolean utilizarRsiMedio;

    @Column(name = "periodo_rsi_medio")
    Integer periodoRsiMedio;

    /*---------- RSI longo ----------*/
    @Column(name = "utilizar_rsi_longo", nullable = false)
    Boolean utilizarRsiLongo;

    @Column(name = "periodo_rsi_longo")
    Integer periodoRsiLongo;

    /*---------- RSI estocástico ----------*/
    @Column(name = "utilizar_rsi_estocastico", nullable = false)
    Boolean utilizarRsiEstocastico;

    @Column(name = "periodo_rsi_estocastico")
    Integer periodoRsiEstocastico;

    @Column(name = "suavizacao_rsi_estocastico_d")
    Integer suavizacaoRsiEstocasticoD;

    @Column(name = "suavizacao_rsi_estocastico_k")
    Integer suavizacaoRsiEstocasticoK;

    /*---------- EMA ----------*/
    @Column(name = "utilizar_ema", nullable = false)
    Boolean utilizarEma;

    @Column(name = "periodo_ema")
    Integer periodoEma;

    /*---------- SMA ----------*/
    @Column(name = "utilizar_sma", nullable = false)
    Boolean utilizarSma;

    @Column(name = "periodo_sma")
    Integer periodoSma;

    /*---------- Volume ----------*/
    @Column(name = "realizar_leitura_volume", nullable = false)
    Boolean realizarLeituraVolume;
}