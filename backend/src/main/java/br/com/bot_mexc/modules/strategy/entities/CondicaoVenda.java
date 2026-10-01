package br.com.bot_mexc.modules.strategy.entities;
import br.com.bot_mexc.shared.enums.*;
import br.com.bot_mexc.shared.utils.DateUtils;
import br.com.bot_mexc.shared.entities.BaseEntity;
import br.com.bot_mexc.shared.configs.RabbitMQConfig;
import br.com.bot_mexc.modules.strategy.entities.*;
import br.com.bot_mexc.modules.strategy.dtos.*;
import br.com.bot_mexc.modules.strategy.repositories.*;
import br.com.bot_mexc.modules.strategy.services.*;
import br.com.bot_mexc.modules.strategy.utils.*;
import br.com.bot_mexc.modules.strategy.builders.*;
import br.com.bot_mexc.modules.strategy.services.OperacaoCacheService;
import br.com.bot_mexc.modules.strategy.services.IndicadorStateService;
import br.com.bot_mexc.modules.market.services.MexcConnectionService;
import br.com.bot_mexc.modules.market.services.mexc.MexcSubscriptionService;
import br.com.bot_mexc.modules.strategy.services.AvaliacaoCondicaoService;
import br.com.bot_mexc.modules.strategy.dtos.OperacaoCacheDTO;
import br.com.bot_mexc.modules.timeseries.repositories.AnaliseRepository;
import br.com.bot_mexc.modules.timeseries.services.BacktestCandleProviderService;
import br.com.bot_mexc.modules.timeseries.dtos.CandleDTO;
import br.com.bot_mexc.modules.timeseries.builders.AnaliseBuilder;
import br.com.bot_mexc.shared.entities.BaseEntity;

import br.com.bot_mexc.shared.enums.OperadorComparacao;
import br.com.bot_mexc.shared.enums.OperadorLogico;
import br.com.bot_mexc.shared.enums.TipoOperando;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = "estrategia")
@ToString(callSuper = true, exclude = "estrategia")
@Table(name = "condicoes_venda")
public class CondicaoVenda extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estrategia", nullable = false)
    @JsonIgnore
    private Estrategia estrategia;

    @Column(name = "ordem", nullable = false)
    private Integer ordem;

    @Enumerated(EnumType.STRING)
    @Column(name = "operador_para_proxima")
    private OperadorLogico operadorParaProxima;

    // --- Operando A ---
    @Enumerated(EnumType.STRING)
    @Column(name = "operando_a_tipo", nullable = false)
    private TipoOperando operandoATipo;

    @Column(name = "operando_a_referencia")
    private String operandoAReferencia;

    @Column(name = "operando_a_valor")
    private BigDecimal operandoAValor;

    // --- Operador de Comparação ---
    @Enumerated(EnumType.STRING)
    @Column(name = "operador", nullable = false)
    private OperadorComparacao operador;

    // --- Operando B ---
    @Enumerated(EnumType.STRING)
    @Column(name = "operando_b_tipo", nullable = false)
    private TipoOperando operandoBTipo;

    @Column(name = "operando_b_referencia")
    private String operandoBReferencia;

    @Column(name = "operando_b_valor")
    private BigDecimal operandoBValor;

}