
package com.app.entities;

import com.app.utiles.BaseEntity;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "portafolios")
@Data
@NoArgsConstructor
@AllArgsConstructor 
@EqualsAndHashCode(callSuper = true)
public class PortafolioEntity extends BaseEntity implements Serializable {

    @Column(name = "nombre_portafolio", nullable = false)
    private String nombrePortafolio;
    
    @Column(name = "descripcion")
    private String descripcion;

    // Relación OneToMany: Un portafolio puede tener muchos registros en la tabla de mapeo
    @OneToMany(mappedBy = "portafolio", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PortafolioTransaccionEntity> portafolioTransacciones = new HashSet<>();

}