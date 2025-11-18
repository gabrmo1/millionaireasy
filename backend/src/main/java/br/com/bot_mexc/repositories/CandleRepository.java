package br.com.bot_mexc.repositories;

import br.com.bot_mexc.models.entities.Candle;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
public interface CandleRepository extends JpaRepository<Candle, String> {

    Optional<Candle> findTopByParAndIntervaloOrderByDataFechamentoDesc(String par, String intervalo);

    Optional<Candle> findByParAndIntervaloAndDataFechamento(String par, String intervalo, LocalDateTime dataFechamento);

    @Query("SELECT c FROM Candle c " +
            "WHERE c.par = :par AND c.intervalo = :intervalo " +
            "ORDER BY c.dataFechamento DESC")
    List<Candle> findTopCandlesDesc(@Param("par") String par,
                                    @Param("intervalo") String intervalo,
                                    Pageable pageable);

    default List<Candle> findLast200CandlesAsc(String par, String intervalo) {
        List<Candle> candles = new ArrayList<>(findTopCandlesDesc(par, intervalo, PageRequest.of(0, 200)));
        candles.sort(Comparator.comparing(Candle::getDataFechamento));
        return candles;
    }

}