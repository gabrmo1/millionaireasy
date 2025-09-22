package br.com.bot_mexc.models.entities;

import br.com.bot_mexc.configs.security.StringCryptoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "operadores")
@EqualsAndHashCode(callSuper = true)
public class Operador extends BaseEntity {

    @Column(name = "nome", nullable = false, length = 50)
    String nome;

    @Column(name = "access_key", nullable = false)
    String accessKey;

    @Column(name = "secret_key", nullable = false)
    @Convert(converter = StringCryptoConverter.class)
    String secretKey;

}