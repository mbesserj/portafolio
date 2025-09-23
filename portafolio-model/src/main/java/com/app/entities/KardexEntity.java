package com.app.entities;

import com.app.enums.TipoEnumsCosteo;
import com.app.utiles.BaseEntity;
import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "kardex")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class KardexEntity extends BaseEntity implements Serializable {

    @Column(name = "fecha_transaccion", nullable = false)
    LocalDate fechaTransaccion;
        
    @Column(name = "clave_agrupacion", length = 255)
    private String claveAgrupacion;
    
    @Column(name = "fecha_costeo", nullable = false)
    private LocalDate fechaCosteo;
    
    @Column(name = "folio", length = 50) 
    private String folio;

    @Column(name = "costo_unitario")
    private BigDecimal costoUnitario;

    @Column(name = "costo_total")
    private BigDecimal costoTotal;

    @Column(name = "saldo_cantidad")
    private BigDecimal saldoCantidad;

    @Column(name = "saldo_valor")
    private BigDecimal saldoValor;

    @Column(name = "cuenta")
    private String cuenta;

    @Column(name = "cantidad", nullable = false)
    private BigDecimal cantidad;

    @Column(name = "cantidad_disponible")
    private BigDecimal cantidadDisponible;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_contable", nullable = false)
    private TipoEnumsCosteo tipoContable;

    @ManyToOne
    @JoinColumn(name = "transaccion_id", nullable = false)
    private TransaccionEntity transaccion;

    @ManyToOne
    @JoinColumn(name = "custodio_id", nullable = false)
    private CustodioEntity custodio;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private EmpresaEntity empresa;

    @ManyToOne
    @JoinColumn(name = "instrumento_id", nullable = false)
    private InstrumentoEntity instrumento;

    @PrePersist
    public void onPrePersist() {
        this.fechaCosteo = LocalDate.now();
    }
}