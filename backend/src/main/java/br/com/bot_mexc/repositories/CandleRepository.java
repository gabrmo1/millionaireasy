package br.com.bot_mexc.repositories;

import br.com.bot_mexc.models.entities.Candle;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Repository
public interface CandleRepository extends JpaRepository<Candle, String> {

    @Query("SELECT c FROM Candle c " +
            "WHERE c.par = :par AND c.intervalo = :intervalo " +
            "ORDER BY c.dataFechamento DESC")
    List<Candle> findTopCandlesDesc(@Param("par") String par,
                                    @Param("intervalo") String intervalo,
                                    Pageable pageable);

    boolean existsByParAndIntervaloAndDataFechamento(String par, String intervalo, Instant localDateTime);

    /**
     * Extração cirúrgica do Maximum Adverse Excursion (MAE).
     * Delega a computação de agregação MIN() diretamente para o banco de dados (Mechanical Sympathy),
     * prevenindo o carregamento de milhares de instâncias de Candle na heap (OOM).
     */
    @Query("SELECT MIN(c.minima) FROM Candle c WHERE c.par = :par AND c.intervalo = :intervalo AND c.dataAbertura >= :dataInicio AND c.dataFechamento <= :dataFim")
    BigDecimal findMinPriceBetween(
            @Param("par") String par,
            @Param("intervalo") String intervalo,
            @Param("dataInicio") Instant dataInicio,
            @Param("dataFim") Instant dataFim
    );

    @Query("SELECT c FROM Candle c WHERE c.par = :par AND c.intervalo = :intervalo AND c.dataAbertura >= :inicio AND c.dataAbertura <= :fim ORDER BY c.dataAbertura ASC")
    List<Candle> findByParAndIntervaloAndDataAberturaBetweenOrderByDataAberturaAsc(
            @Param("par") String par,
            @Param("intervalo") String intervalo,
            @Param("inicio") Instant inicio,
            @Param("fim") Instant fim
    );

}