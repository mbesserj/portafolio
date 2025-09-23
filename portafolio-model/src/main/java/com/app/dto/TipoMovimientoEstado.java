
package com.app.dto;

import com.app.enums.TipoEnumsCosteo; // <-- IMPORTANTE: Importa tu enum
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor 
public class TipoMovimientoEstado {

    private Long id;
    private String descripcion;
    private String movimiento;
    private TipoEnumsCosteo estado;
}