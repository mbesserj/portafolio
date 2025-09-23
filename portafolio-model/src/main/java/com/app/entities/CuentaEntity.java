package com.app.entities;

import com.app.utiles.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cuentas", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"cuenta", "empresa_id", "custodio_id"}, name = "uk_cuenta_empresa_custodio")
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = {"empresa", "custodio"})
public class CuentaEntity extends BaseEntity {

    @Column(name = "cuenta", nullable = false)
    private String cuenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private EmpresaEntity empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custodio_id", nullable = false)
    private CustodioEntity custodio;

    public CuentaEntity(String cuenta, EmpresaEntity empresa, CustodioEntity custodio) {
        this.cuenta = cuenta;
        this.empresa = empresa;
        this.custodio = custodio;
    }
}