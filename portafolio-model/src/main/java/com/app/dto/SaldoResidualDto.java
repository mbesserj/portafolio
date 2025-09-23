package com.app.dto;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@SqlResultSetMapping(
    name = "Mapping.SaldoResidualDto",
    classes = @ConstructorResult(
        targetClass = SaldoResidualDto.class,
        columns = {
            @ColumnResult(name = "empresa_id", type = Long.class),
            @ColumnResult(name = "custodio_id", type = Long.class),
            @ColumnResult(name = "instrumento_id", type = Long.class),
            @ColumnResult(name = "cuenta", type = String.class),
            @ColumnResult(name = "saldoCantidad", type = BigDecimal.class),
            @ColumnResult(name = "saldoValor", type = BigDecimal.class)
        }
    )
)
@Data
@Getter 
@NoArgsConstructor 
public class SaldoResidualDto {
    
    @Id // JPA requiere un @Id para la proyección de resultados
    private Long instrumento_id; 
    private Long empresa_id;
    private Long custodio_id;
    
    private String cuenta;
    private BigDecimal saldoCantidad;
    private BigDecimal saldoValor;

    // Constructor que coincide con el @SqlResultSetMapping
    public SaldoResidualDto(Long empresa_id, Long custodio_id, Long instrumento_id, String cuenta, BigDecimal saldoCantidad, BigDecimal saldoValor) {
        this.empresa_id = empresa_id;
        this.custodio_id = custodio_id;
        this.instrumento_id = instrumento_id;
        this.cuenta = cuenta;
        this.saldoCantidad = saldoCantidad;
        this.saldoValor = saldoValor;
    }
}