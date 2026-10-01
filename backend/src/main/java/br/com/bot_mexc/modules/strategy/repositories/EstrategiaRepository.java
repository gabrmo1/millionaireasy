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

import br.com.bot_mexc.modules.strategy.entities.Estrategia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstrategiaRepository extends JpaRepository<Estrategia, String> {

    @Query("SELECT e FROM Estrategia e LEFT JOIN FETCH e.indicadoresConfig LEFT JOIN FETCH e.condicoesCompra LEFT JOIN FETCH e.condicoesVenda WHERE e.id = :id")
    Optional<Estrategia> findByIdWithConditions(@Param("id") String id);

    @Query("SELECT DISTINCT e FROM Estrategia e LEFT JOIN FETCH e.indicadoresConfig LEFT JOIN FETCH e.condicoesCompra LEFT JOIN FETCH e.condicoesVenda")
    List<Estrategia> findAllWithConditions();
}