package br.com.bot_mexc.repositories;

import br.com.bot_mexc.models.entities.Operacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperacaoRepository extends JpaRepository<Operacao, String> {

    @Query("SELECT o FROM Operacao o JOIN FETCH o.operador LEFT JOIN FETCH o.estrategia")
    List<Operacao> findAllEagerly();

}