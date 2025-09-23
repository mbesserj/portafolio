
package com.app.entities;

import com.app.utiles.BaseEntity;
import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "cierres_contables",
    uniqueConstraints = @UniqueConstraint(columnNames = {"ejercicio", "empresa_id", "custodio_id", "instrumento_id"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class CierreContableEntity extends BaseEntity implements Serializable {

    @Column(name = "ejercicio", nullable = false)
    private int ejercicio; // El año del cierre, ej: 2024

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private EmpresaEntity empresa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "custodio_id", nullable = false)
    private CustodioEntity custodio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrumento_id", nullable = false)
    private InstrumentoEntity instrumento;

    @Column(name = "cantidad_cierre", nullable = false, precision = 18, scale = 4)
    private BigDecimal cantidadCierre;

    @Column(name = "valor_cierre", nullable = false, precision = 18, scale = 4)
    private BigDecimal valorCierre;
}
