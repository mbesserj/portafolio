
package com.app.entities;

import com.app.utiles.BaseEntity;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = "perfiles")
public class UsuarioEntity extends BaseEntity implements Serializable {

    @Column(name = "usuario", unique = true, nullable = false)
    private String usuario;
    
    @Column(name = "correo", unique = true)
    private String correo;
    
    @Column(name = "password", nullable = false)
    private String password;
    
    @Column(name = "fecha_inactivo")
    private LocalDate fechaInactivo;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "usuarios_perfiles",
        joinColumns = @JoinColumn(name = "usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "perfil_id")
    )
    private Set<PerfilEntity> perfiles = new HashSet<>();

    @Override
    public String toString() {
        return "UsuarioEntity{" +
               "id=" + getId() +
               ", usuario='" + usuario + '\'' +
               ", email='" + correo + '\'' +
               '}';
    }
}