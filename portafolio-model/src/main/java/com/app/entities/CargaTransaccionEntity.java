
package com.app.entities;

import com.app.utiles.Pk;
import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import static java.time.LocalDate.now;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "carga_transacciones")
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
public class CargaTransaccionEntity implements Serializable {

    @EmbeddedId
    private Pk id;

    // Campos de negocio
    @Column(name = "razon_social")
    private String razonSocial;

    @Column(name = "rut")
    private String rut;

    @Column(name = "cuenta")
    private String cuenta;

    @Column(name = "custodio")
    private String custodioNombre;

    @Column(name = "folio")
    private String folio;

    @Column(name = "tipo_movimiento")
    private String tipoMovimiento;
    
    @Column(name = "movimiento_caja")
    private String movimientoCaja;

    @Column(name = "producto")
    private String producto;

    @Column(name = "instrumento_nemo")
    private String instrumentoNemo;

    @Column(name = "instrumento_nombre")
    private String instrumentoNombre;

    @Column(name = "moneda")
    private String moneda;

    @Column(name = "cantidad")
    private BigDecimal cantidad;

    @Column(name = "precio")
    private BigDecimal precio;

    @Column(name = "monto")
    private BigDecimal monto;

    @Column(name = "monto_total")
    private BigDecimal montoTotal;

    @Column(name = "comision")
    private BigDecimal comisiones;

    @Column(name = "gastos")
    private BigDecimal gastos;

    @Column(name = "iva")
    private BigDecimal iva;

    @Column(name = "cuenta_psh")
    private String cuentaPsh;

    @Column(name = "cant_libre")
    private BigDecimal cantLibre;

    @Column(name = "cant_garantia")
    private BigDecimal cantGarantia;

    @Column(name = "cant_plazo")
    private BigDecimal cantPlazo;

    @Column(name = "cant_vc")
    private BigDecimal cantVc;

    @Column(name = "cant_total")
    private BigDecimal cantTotal;

    @Column(name = "monto_clp")
    private BigDecimal montoClp;

    @Column(name = "monto_usd")
    private BigDecimal montoUsd;
    
    // Valor de control de procesado
    @Column(name = "procesado", nullable = false)
    private boolean procesado = false; 
    
    // Campos de auditoría
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDate fechaCreacion = now();

    @Column(name = "fecha_modificacion")
    private LocalDate fechaModificacion;

    @Column(name = "creado_por", nullable = false, updatable = false)
    private String creadoPor = "sistema";

    @Column(name = "modificado_por")
    private String modificadoPor = "sistema";

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDate.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.fechaModificacion = LocalDate.now();
    }
}