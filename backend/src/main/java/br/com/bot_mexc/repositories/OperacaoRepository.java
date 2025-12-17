package br.com.bot_mexc.repositories;

import br.com.bot_mexc.models.entities.Operacao;
import br.com.bot_mexc.models.enums.StatusOperacoes;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // IMPORT ADICIONADO
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OperacaoRepository extends JpaRepository<Operacao, String> {

    @Query("SELECT o FROM Operacao o LEFT JOIN FETCH o.operador LEFT JOIN FETCH o.estrategia")
    List<Operacao> findAllEagerly();

    List<Operacao> findByStatus(StatusOperacoes status);

    @Query("SELECT o FROM Operacao o " +
            "LEFT JOIN FETCH o.estrategia e " +
            "LEFT JOIN FETCH e.condicoesCompra " +
            "LEFT JOIN FETCH e.condicoesVenda " +
            "LEFT JOIN FETCH e.indicadoresConfig " +
            "WHERE o.status = :status AND o.par = :par AND o.intervalo = :intervalo")
    List<Operacao> findActiveOperationsEagerly(
            @Param("status") StatusOperacoes status,
            @Param("par") String par,
            @Param("intervalo") String intervalo
    );

    @Modifying
    @Transactional
    @Query("UPDATE Operacao o SET o.saldoInicial = :novoSaldo WHERE o.id = :id")
    void atualizarSaldo(String id, BigDecimal novoSaldo);
}