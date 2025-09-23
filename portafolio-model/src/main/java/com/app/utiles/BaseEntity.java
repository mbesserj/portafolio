package com.app.utiles;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Data;

@MappedSuperclass
@Data
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    public LocalDate fechaCreacion;

    @Column(name = "fecha_modificacion")
    public LocalDate fechaModificacion;

    @Column(name = "creado_por", nullable = false, updatable = false)
    public String creadoPor;

    @Column(name = "modificado_por")
    public String modificadoPor;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDate.now();
        creadoPor = "sistema";
    }

    @PreUpdate
    protected void onUpdate() {
        fechaModificacion = LocalDate.now();
        modificadoPor = "sistema";
    }
}