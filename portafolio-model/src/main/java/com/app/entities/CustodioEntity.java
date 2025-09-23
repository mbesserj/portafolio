package com.app.entities;

import com.app.utiles.BaseEntity;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "custodios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = {"empresas", "cuentas", "transacciones"})
public class CustodioEntity extends BaseEntity implements Serializable {

    @Column(name = "custodio", nullable = false)
    private String nombreCustodio;

    @ManyToMany(mappedBy = "custodios", fetch = FetchType.LAZY)
    private Set<EmpresaEntity> empresas = new HashSet<>();

    @OneToMany(mappedBy = "custodio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransaccionEntity> transacciones = new LinkedList<>();

    @OneToMany(mappedBy = "custodio", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CuentaEntity> cuentas = new HashSet<>();

    @Override
    public String toString() {
        return nombreCustodio; 
    }

}
