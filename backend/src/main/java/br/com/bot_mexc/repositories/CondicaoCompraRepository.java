package br.com.bot_mexc.repositories;

import br.com.bot_mexc.models.entities.CondicaoCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CondicaoCompraRepository extends JpaRepository<CondicaoCompra, String> {
}