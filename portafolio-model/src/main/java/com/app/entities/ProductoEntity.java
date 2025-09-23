package com.app.entities;

import com.app.utiles.BaseEntity;
import jakarta.persistence.*;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Corresponde a las clasificaciones de los activos por ejemplo fondos mutuos,
 * acciones, forwards, etc.
 */
@Entity
@Table(name = "productos")
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@EqualsAndHashCode(callSuper = true) 
public class ProductoEntity extends BaseEntity implements Serializable {

    @Column(name = "producto", nullable = false, unique = true)
    private String producto;
    
    @Column(name = "detalle_producto")
    private String detalleProducto;

}