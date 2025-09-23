package br.com.bot_mexc.repositories;

import br.com.bot_mexc.models.entities.Estrategia;
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