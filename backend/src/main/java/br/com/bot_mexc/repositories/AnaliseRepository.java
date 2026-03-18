package br.com.bot_mexc.repositories;

import br.com.bot_mexc.configs.annotations.IgnoreTenantFilter;
import br.com.bot_mexc.models.entities.Analise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AnaliseRepository extends JpaRepository<Analise, String> {

    @IgnoreTenantFilter
    @Query("""
                SELECT a FROM Analise a
                WHERE a.par = :par
                  AND a.intervalo = :intervalo
                  AND a.dataAnalise BETWEEN :inicio AND :fim
                  AND (:periodoRsiCurto = 0 OR a.periodoRsiCurto = :periodoRsiCurto)
                  AND (:periodoRsiMedio = 0 OR a.periodoRsiMedio = :periodoRsiMedio)
                  AND (:periodoRsiLongo = 0 OR a.periodoRsiLongo = :periodoRsiLongo)
                  AND (:periodoRsiEstocastico = 0 OR a.periodoRsiEstocastico = :periodoRsiEstocastico)
                  AND (:suavizacaoK = 0 OR a.suavizacaoRsiEstocasticoK = :suavizacaoK)
                  AND (:suavizacaoD = 0 OR a.suavizacaoRsiEstocasticoD = :suavizacaoD)
                  AND (:periodoEma IS NULL OR a.periodoEma = :periodoEma)
                  AND (:periodoSma IS NULL OR a.periodoSma = :periodoSma)
                ORDER BY a.dataAnalise ASC
            """)
    List<Analise> buscarAnalisesCompativeis(
            String par,
            String intervalo,
            Instant inicio,
            Instant fim,
            Integer periodoRsiCurto,
            Integer periodoRsiMedio,
            Integer periodoRsiLongo,
            Integer periodoRsiEstocastico,
            Integer suavizacaoK,
            Integer suavizacaoD,
            Integer periodoEma,
            Integer periodoSma
    );
}