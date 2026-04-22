package br.com.bot_mexc.repositories;

import br.com.bot_mexc.models.entities.Venda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface VendaRepository extends JpaRepository<Venda, String> {

    boolean existsByOperacaoIdAndDataCriacaoAfter(String operacaoId, Instant data);

    List<Venda> findAllByOperacaoIdOrderByDataCriacaoDesc(String idOperacao);

}