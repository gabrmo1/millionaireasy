package br.com.bot_mexc.models.entities;

import br.com.bot_mexc.models.enums.TipoIndicador;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = "estrategia")
@ToString(callSuper = true, exclude = "estrategia")
@Table(name = "indicadores_config")
public class IndicadorConfig extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estrategia", nullable = false)
    private Estrategia estrategia;

    @Column(name = "alias", nullable = false)
    private String alias;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_indicador", nullable = false)
    private TipoIndicador tipoIndicador;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parametros", columnDefinition = "jsonb")
    private String parametros;
}