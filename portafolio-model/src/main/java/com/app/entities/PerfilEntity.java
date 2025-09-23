
package com.app.entities;

import com.app.utiles.BaseEntity;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "perfiles")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = "usuarios")
public class PerfilEntity extends BaseEntity implements Serializable {

    @Column(name = "perfil", unique = true, nullable = false)
    private String perfil; // Ej: "ADMINISTRADOR", "OPERADOR"

    @ManyToMany(mappedBy = "perfiles", fetch = FetchType.LAZY)
    private Set<UsuarioEntity> usuarios = new HashSet<>();
    
    public PerfilEntity(String perfil) {
        this.perfil = perfil;
    }

    @Override
    public String toString() {
        return "PerfilEntity{" +
               "id=" + getId() +
               ", nombre='" + perfil + '\'' +
               '}';
    }
}