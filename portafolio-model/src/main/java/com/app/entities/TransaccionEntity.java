package com.app.entities;

import com.app.dto.CargaTransaccion;
import com.app.utiles.BaseEntity;
import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Entidad JPA para la tabla 'transacciones'. Esta clase representa la
 * transacción normalizada y lista para ser procesada.
 */
@Entity
@Table(name = "transacciones")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TransaccionEntity extends BaseEntity implements Serializable {

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "folio", length = 50)
    private String folio;

    @Column(name = "cuenta")
    private String cuenta;

    @Column(name = "glosa")
    private String glosa;

    @Column(name = "cantidad", precision = 19, scale = 6)
    private BigDecimal cantidad;

    @Column(name = "precio", precision = 19, scale = 6)
    private BigDecimal precio;

    @Column(name = "comision", precision = 19, scale = 6)
    private BigDecimal comisiones;

    @Column(name = "gasto", precision = 19, scale = 6)
    private BigDecimal gastos;

    @Column(name = "iva", precision = 19, scale = 6)
    private BigDecimal iva;

    @Column(name = "total", precision = 19, scale = 6)
    private BigDecimal total;

    @Column(name = "monto", precision = 19, scale = 6)
    private BigDecimal monto;

    @Column(name = "monto_clp", precision = 19, scale = 6)
    private BigDecimal montoClp;

    @Column(name = "moneda", length = 10)
    private String moneda;

    @Column(name = "costeado")
    private boolean costeado;

    @Column(name = "para_revision")
    private boolean paraRevision = false;
    
    @Column(name = "ignorar_en_costeo")
    private boolean ignorarEnCosteo = false;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private EmpresaEntity empresa;

    @ManyToOne
    @JoinColumn(name = "instrumento_id", nullable = false)
    private InstrumentoEntity instrumento;

    @ManyToOne
    @JoinColumn(name = "custodio_id", nullable = false)
    private CustodioEntity custodio;

    @ManyToOne
    @JoinColumn(name = "movimiento_id")
    private TipoMovimientoEntity tipoMovimiento;

    // constructor dto to entity
    public TransaccionEntity(CargaTransaccion dto,
            EmpresaEntity empresa,
            CustodioEntity custodio,
            ProductoEntity producto,
            InstrumentoEntity instrumento,
            TipoMovimientoEntity tipoMovimiento) {
        this.fecha = dto.getTransactionDate();
        this.folio = dto.getFolio();
        this.cuenta = dto.getCuenta();
        this.cantidad = dto.getCantidad();
        this.precio = dto.getPrecio();
        this.comisiones = dto.getComisiones();
        this.gastos = dto.getGastos();
        this.iva = dto.getIva();
        this.total = dto.getMontoTotal();
        this.monto = dto.getMonto();
        this.montoClp = dto.getMontoClp();
        this.moneda = dto.getMoneda();
        this.costeado = false;
        this.empresa = empresa;
        this.instrumento = instrumento;
        this.custodio = custodio;
        this.tipoMovimiento = tipoMovimiento;
        this.paraRevision = false;
        this.ignorarEnCosteo = false;
    }

    /**
     * Este método se ejecutará automáticamente ANTES de que la entidad se
     * guarde por primera vez (persist) o se actualice (update). Su función es
     * asegurar que el campo 'total' siempre esté calculado.
     */
    @PrePersist
    @PreUpdate
    public void calcularTotal() {
        if (this.cantidad != null && this.precio != null) {
            // Si hay cantidad y precio, calcula el total
            this.total = this.cantidad.multiply(this.precio);
        } else if (this.total == null) {
            // Si no se puede calcular y el total es nulo, asegúralo en CERO
            this.total = BigDecimal.ZERO;
        }
    }
}