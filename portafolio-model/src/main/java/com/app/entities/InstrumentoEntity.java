package com.app.entities;

import com.app.utiles.BaseEntity;
import jakarta.persistence.*;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "instrumentos",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"instrumento_nemo"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InstrumentoEntity extends BaseEntity implements Serializable {

    @Column(name = "nemo", nullable = false, unique = true)
    private String instrumentoNemo;

    @Column(name = "instrumento", nullable = false)
    private String instrumentoNombre;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id", referencedColumnName = "id", nullable = false)
    private ProductoEntity producto;

}