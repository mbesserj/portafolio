package com.app.entities;

import com.app.utiles.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "saldos_diarios", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"fecha", "empresa_id", "custodio_id", "instrumento_id", "cuenta"})
})
@Data
@NoArgsConstructor 
@AllArgsConstructor 
@EqualsAndHashCode(callSuper = true)
public class SaldosDiariosEntity extends BaseEntity implements Serializable {

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "cuenta")
    private String cuenta;

    @Column(name = "saldo_cantidad", precision = 19, scale = 6)
    private BigDecimal saldoCantidad;

    @Column(name = "saldo_valor", precision = 19, scale = 6)
    private BigDecimal saldoValor;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private EmpresaEntity empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custodio_id", nullable = false)
    private CustodioEntity custodio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrumento_id", nullable = false)
    private InstrumentoEntity instrumento;
}