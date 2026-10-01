package br.com.bot_mexc.modules.timeseries.entities;
import br.com.bot_mexc.shared.enums.*;
import br.com.bot_mexc.shared.utils.DateUtils;
import br.com.bot_mexc.shared.entities.BaseEntity;
import br.com.bot_mexc.shared.configs.RabbitMQConfig;
import br.com.bot_mexc.modules.timeseries.entities.*;
import br.com.bot_mexc.modules.timeseries.dtos.*;
import br.com.bot_mexc.modules.timeseries.repositories.*;
import br.com.bot_mexc.modules.timeseries.services.*;
import br.com.bot_mexc.modules.timeseries.utils.*;
import br.com.bot_mexc.modules.timeseries.builders.*;
import br.com.bot_mexc.modules.strategy.entities.IndicadorConfig;
import br.com.bot_mexc.shared.entities.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "analises")
@EqualsAndHashCode(callSuper = true)
public class Analise extends BaseEntity {

    @Column(name = "par", length = 20, nullable = false)
    String par;

    @Column(name = "intervalo", nullable = false)
    String intervalo;

    @Column(name = "valor_atual_moeda", nullable = false)
    BigDecimal valorAtualMoeda;

    @Column(name = "data_analise", nullable = false)
    Instant dataAnalise;

    @Column(name = "periodo_ema")
    Integer periodoEma;

    @Column(name = "periodo_sma")
    Integer periodoSma;

    @Column(name = "periodo_rsi_curto", nullable = false)
    Integer periodoRsiCurto;

    @Column(name = "periodo_rsi_medio", nullable = false)
    Integer periodoRsiMedio;

    @Column(name = "periodo_rsi_longo", nullable = false)
    Integer periodoRsiLongo;

    @Column(name = "periodo_rsi_estocastico", nullable = false)
    Integer periodoRsiEstocastico;

    @Column(name = "suavizacao_rsi_estocastico_d", nullable = false)
    Integer suavizacaoRsiEstocasticoD;

    @Column(name = "suavizacao_rsi_estocastico_k", nullable = false)
    Integer suavizacaoRsiEstocasticoK;

    @Column(name = "ema", nullable = false)
    BigDecimal ema;

    @Column(name = "sma", nullable = false)
    BigDecimal sma;

    @Column(name = "rsi_curto", nullable = false)
    BigDecimal rsiCurto;

    @Column(name = "rsi_medio", nullable = false)
    BigDecimal rsiMedio;

    @Column(name = "rsi_longo", nullable = false)
    BigDecimal rsiLongo;

    @Column(name = "rsi_estocastico_d", nullable = false)
    BigDecimal rsiEstocasticoD;

    @Column(name = "rsi_estocastico_k", nullable = false)
    BigDecimal rsiEstocasticoK;

    @Column(name = "volume", nullable = false)
    BigDecimal volume;

}