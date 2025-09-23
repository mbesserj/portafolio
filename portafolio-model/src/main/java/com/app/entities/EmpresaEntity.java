package com.app.entities;

import com.app.utiles.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "empresas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = {"custodios", "cuentas", "transacciones"})
public class EmpresaEntity extends BaseEntity implements Serializable {

    @NotBlank(message = "El RUT no puede estar vacío.")
    @Size(max = 14, message = "El RUT no puede tener más de 14 caracteres.")
    @Column(name = "rut", unique = true, nullable = false)
    private String rut;

    @NotBlank(message = "La Razón Social no puede estar vacía.")
    @Size(max = 255, message = "La Razón Social no puede tener más de 255 caracteres.")
    @Column(name = "razonsocial", nullable = false)
    private String razonSocial;

    @Column(name = "fecha_creado", nullable = false)
    private LocalDate fechaCreado = LocalDate.now();

    @OneToMany(mappedBy = "empresa", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransaccionEntity> transacciones = new LinkedList<>();

    @ManyToOne(optional = true)
    @JoinColumn(name = "grupo_empresa_id")
    private GrupoEmpresaEntity grupoEmpresa;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "empresa_custodio",
            joinColumns = @JoinColumn(name = "empresa_id"),
            inverseJoinColumns = @JoinColumn(name = "custodio_id")
    )
    private Set<CustodioEntity> custodios = new HashSet<>();

    @OneToMany(mappedBy = "empresa", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CuentaEntity> cuentas = new HashSet<>();

    @Override
    public String toString() {
        return razonSocial; 
    }

}
