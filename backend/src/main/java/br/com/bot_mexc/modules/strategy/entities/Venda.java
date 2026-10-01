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
@Table(name = "vendas")
@EqualsAndHashCode(callSuper = true)
public class Venda extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_operacao", nullable = false)
    Operacao operacao;

    @Column(name = "data_venda", nullable = false)
    Instant dataVenda;

    @Column(name = "data_candle")
    Instant dataCandle;

    @Column(name = "valor_compra", nullable = false)
    BigDecimal valorCompra;

    @Column(name = "valor_venda", nullable = false)
    BigDecimal valorVenda;

    @Column(name = "lucro", nullable = false)
    BigDecimal lucro;

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