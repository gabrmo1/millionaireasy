package br.com.bot_mexc.repositories;

import br.com.bot_mexc.models.entities.Candle;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}