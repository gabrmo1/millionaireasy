package br.com.bot_mexc.repositories;

import br.com.bot_mexc.models.entities.Candle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CandleRepository extends JpaRepository<Candle, String> {

    Optional<Candle> findTopByParAndIntervaloOrderByDataFechamentoDesc(String par, String intervalo);

    Optional<Candle> findByParAndIntervaloAndDataFechamento(String par, String intervalo, LocalDateTime dataFechamento);

}
