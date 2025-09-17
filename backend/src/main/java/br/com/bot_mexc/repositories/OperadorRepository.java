package br.com.bot_mexc.repositories;

import br.com.bot_mexc.models.entities.Operador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OperadorRepository extends JpaRepository<Operador, String> {

    Optional<Operador> findByAccessKey(String accessKey);

}