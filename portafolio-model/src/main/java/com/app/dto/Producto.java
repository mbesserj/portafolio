package com.app.dto;

import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Corresponde a las clasificaciones de los activos
 * por ejemplo fondos mutuos, acciones, forwards, etc.
 */
@Data // Genera getters, setters, toString, equals y hashCode
@NoArgsConstructor // Genera constructor sin argumentos
public class Producto {

    private Long id;
    private String producto;
    private LocalDate fechaCreado;

    public Producto(String producto) {
        this.producto = producto;
    }    

    public Producto(Long id, String producto, LocalDate fechaCreado) {
        this.id = id;
        this.producto = producto;
        this.fechaCreado = fechaCreado;
    }
}