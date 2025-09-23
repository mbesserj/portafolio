
package com.app.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para transportar los valores calculados para una propuesta de ajuste manual.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValoresAjusteDto {

    private BigDecimal cantidad;
    private BigDecimal precio;

}
