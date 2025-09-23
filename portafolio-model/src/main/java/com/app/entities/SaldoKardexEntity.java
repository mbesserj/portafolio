package com.app.entities; // O el paquete que corresponda

import com.app.utiles.BaseEntity;
import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "saldos_kardex", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"empresa_id", "custodio_id", "instrumento_id", "cuenta"}, name = "uk_saldo_kardex_grupo")
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SaldoKardexEntity extends BaseEntity implements Serializable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private EmpresaEntity empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custodio_id", nullable = false)
    private CustodioEntity custodio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrumento_id", nullable = false)
    private InstrumentoEntity instrumento;
    
    @Column(name = "cuenta", nullable = false)
    private String cuenta;

    @Column(name = "saldo_cantidad", precision = 19, scale = 6, nullable = false)
    private BigDecimal saldoCantidad = BigDecimal.ZERO;

    @Column(name = "costo_total", precision = 19, scale = 6, nullable = false)
    private BigDecimal costoTotal = BigDecimal.ZERO;

    @Column(name = "costo_promedio", precision = 19, scale = 6, nullable = false)
    private BigDecimal costoPromedio = BigDecimal.ZERO;
    
    @Column(name = "fecha_ultima_actualizacion", nullable = false)
    private LocalDate fechaUltimaActualizacion;

    /**
     * Método de ayuda para mantener el costo promedio calculado.
     */
    public void recalcularCostoPromedio() {
        if (this.saldoCantidad != null && this.saldoCantidad.compareTo(BigDecimal.ZERO) != 0) {
            this.costoPromedio = this.costoTotal.divide(this.saldoCantidad, 6, RoundingMode.HALF_UP);
        } else {
            this.costoPromedio = BigDecimal.ZERO;
        }
    }
}