package com.app.dto;

import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Esta entidad contiene las operaciones o tipos de transacciones que se realizan
 * con los instrumentos financieros.
 */
@Data 
@NoArgsConstructor 
public class Operacion {

    private Long id;
    private String operacion;
    private LocalDate fechaCreado;

    public Operacion(Long id, String operacion, LocalDate fechaCreado) {
        this.id = id;
        this.operacion = operacion;
        this.fechaCreado = fechaCreado;
    }
}
