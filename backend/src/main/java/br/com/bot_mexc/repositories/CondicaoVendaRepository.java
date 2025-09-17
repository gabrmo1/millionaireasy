package br.com.bot_mexc.repositories;

import br.com.bot_mexc.models.entities.CondicaoVenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CondicaoVendaRepository extends JpaRepository<CondicaoVenda, String> {
}