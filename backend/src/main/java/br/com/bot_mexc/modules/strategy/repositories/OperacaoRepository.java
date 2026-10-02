package br.com.bot_mexc.modules.strategy.repositories;
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

import br.com.bot_mexc.modules.strategy.entities.Operacao;
import br.com.bot_mexc.shared.enums.StatusOperacoes;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OperacaoRepository extends JpaRepository<Operacao, String> {

    @Query("SELECT o FROM Operacao o LEFT JOIN FETCH o.operador LEFT JOIN FETCH o.estrategia")
    List<Operacao> findAllEagerly();

    @Query("SELECT o FROM Operacao o " +
            "LEFT JOIN FETCH o.operador " +
            "LEFT JOIN FETCH o.estrategia " +
            "WHERE (o.tipoOperacao IS NULL OR o.tipoOperacao != br.com.bot_mexc.shared.enums.TipoOperacao.BACKTEST)")
    List<Operacao> findAllRealOperationsEagerly();

    @Query("SELECT o FROM Operacao o " +
            "LEFT JOIN FETCH o.operador " +
            "LEFT JOIN FETCH o.estrategia " +
            "WHERE o.tipoOperacao = br.com.bot_mexc.shared.enums.TipoOperacao.BACKTEST")
    List<Operacao> findSimulationsEagerly();


    List<Operacao> findByStatus(StatusOperacoes status);

    @Query("SELECT o FROM Operacao o " +
            "LEFT JOIN FETCH o.estrategia e " +
            "LEFT JOIN FETCH e.condicoesCompra " +
            "LEFT JOIN FETCH e.condicoesVenda " +
            "LEFT JOIN FETCH e.indicadoresConfig " +
            "WHERE o.status = :status AND o.par = :par AND o.intervalo = :intervalo")
    List<Operacao> findActiveOperationsEagerly(
            @Param("status") StatusOperacoes status,
            @Param("par") String par,
            @Param("intervalo") String intervalo
    );

    @Query("SELECT o FROM Operacao o " +
            "LEFT JOIN FETCH o.estrategia e " +
            "LEFT JOIN FETCH e.condicoesCompra " +
            "LEFT JOIN FETCH e.condicoesVenda " +
            "LEFT JOIN FETCH e.indicadoresConfig " +
            "WHERE o.id = :id")
    Optional<Operacao> findByIdEagerly(@Param("id") String id);

    @Modifying
    @Transactional
    @Query("UPDATE Operacao o SET o.saldoInicial = :novoSaldo WHERE o.id = :id")
    void atualizarSaldo(String id, BigDecimal novoSaldo);
}