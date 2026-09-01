package br.com.bot_mexc.repositories;

import br.com.bot_mexc.models.entities.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompraRepository extends JpaRepository<Compra, String> {

    Optional<Compra> findTopByOperacaoIdOrderByDataCriacaoDesc(String idOperacao);

    List<Compra> findAllByOperacaoIdOrderByDataCriacaoDesc(String idOperacao);

    List<Compra> findAllByOperacaoIdOrderByDataCompraAsc(String operacaoId);
}