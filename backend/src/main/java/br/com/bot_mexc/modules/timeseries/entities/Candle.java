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

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

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
    Instant dataAbertura;

    @Column(name = "data_fechamento", nullable = false)
    Instant dataFechamento;

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