package com.app.entities;

import com.app.utiles.BaseEntity;
import jakarta.persistence.*;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tipo_movimientos")
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@EqualsAndHashCode(callSuper = true) 
public class TipoMovimientoEntity extends BaseEntity implements Serializable {

    @Column(name = "tipo_movimiento", nullable = false)
    private String tipoMovimiento;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "es_saldo_inicial", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean esSaldoInicial;
        
    @ManyToOne
    @JoinColumn(name = "movimiento_contable_id")
    private MovimientoContableEntity movimientoContable;


    public TipoMovimientoEntity(String tipoMovimiento, String descripcion) {
        this.tipoMovimiento = tipoMovimiento;
        this.descripcion = descripcion;
    }
}