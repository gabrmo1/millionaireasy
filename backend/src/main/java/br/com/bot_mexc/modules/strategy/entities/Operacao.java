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

import br.com.bot_mexc.shared.enums.StatusOperacoes;
import br.com.bot_mexc.shared.enums.TipoOperacao;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

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
    Instant dataInicio;

    @Column(name = "data_fim")
    Instant dataFim;

    @Column(name = "par", length = 20, nullable = false)
    String par;

    @Column(name = "intervalo", nullable = false, length = 5)
    String intervalo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_operador")
    Operador operador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estrategia")
    Estrategia estrategia;

    @Column(name = "modo_teste", nullable = false)
    @Builder.Default
    Boolean modoTeste = false;

    @Column(name = "saldo_inicial")
    BigDecimal saldoInicial;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacao", length = 20, nullable = false)
    @Builder.Default
    TipoOperacao tipoOperacao = TipoOperacao.LIVE;
}