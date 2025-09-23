package com.app.dto;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import lombok.Data;

@SqlResultSetMapping(
        name = "ResumenSaldoEmpresaMapping",
        classes = @ConstructorResult(
                targetClass = ResumenSaldoEmpresaDto.class,
                columns = {
                    @ColumnResult(name = "empresa", type = String.class),
                    @ColumnResult(name = "custodio", type = String.class),
                    @ColumnResult(name = "cuenta", type = String.class),
                    @ColumnResult(name = "saldo_clp", type = BigDecimal.class),
                    @ColumnResult(name = "saldo_usd", type = BigDecimal.class)
                }
        )
)
@Data
public class ResumenSaldoEmpresaDto {

    private String empresa;
    private String custodio;
    private String cuenta;
    private BigDecimal saldoClp;
    private BigDecimal saldoUsd;

    // --- Campos calculados en Java (ignorados por JPA) ---
    @Transient
    private BigDecimal porcentaje;

    @Transient
    private String styleClass;

    // Constructor vacío para crear instancias manualmente (ej. para totales)
    public ResumenSaldoEmpresaDto() {
        this.saldoClp = BigDecimal.ZERO;
        this.saldoUsd = BigDecimal.ZERO;
        this.porcentaje = BigDecimal.ZERO;
    }

    // Constructor que será utilizado por JPA para mapear los resultados de la consulta
    public ResumenSaldoEmpresaDto(String empresa, String custodio, String cuenta, BigDecimal saldoClp, BigDecimal saldoUsd) {
        this.empresa = empresa;
        this.custodio = custodio;
        this.cuenta = cuenta;
        this.saldoClp = saldoClp != null ? saldoClp : BigDecimal.ZERO;
        this.saldoUsd = saldoUsd != null ? saldoUsd : BigDecimal.ZERO;
        this.porcentaje = BigDecimal.ZERO; 
    }
    
    // Lombok generará los getters y setters para todos los campos.

}
