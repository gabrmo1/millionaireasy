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