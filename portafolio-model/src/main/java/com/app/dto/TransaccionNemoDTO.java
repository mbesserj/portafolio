package com.app.dto;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.SqlResultSetMapping;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;

@SqlResultSetMapping(
    name = "TransaccionNemoDTOMapping",
    classes = @ConstructorResult(
        targetClass = TransaccionNemoDTO.class,
        columns = {
            @ColumnResult(name = "fecha_transaccion", type = java.sql.Date.class),
            @ColumnResult(name = "cuenta", type = String.class),
            @ColumnResult(name = "instrumento_nemo", type = String.class),
            @ColumnResult(name = "instrumento_nombre", type = String.class),
            @ColumnResult(name = "ingreso_cantidad", type = BigDecimal.class),
            @ColumnResult(name = "egreso_cantidad", type = BigDecimal.class),
            @ColumnResult(name = "saldo_cantidad_acumulada", type = BigDecimal.class),
            @ColumnResult(name = "ingreso_valor", type = BigDecimal.class),
            @ColumnResult(name = "egreso_valor", type = BigDecimal.class),
            @ColumnResult(name = "utilidad_perdida", type = BigDecimal.class)
        }
    )
)
@Data
@NoArgsConstructor
public class TransaccionNemoDTO {

    private LocalDate fechaTransaccion;
    private String cuenta;
    private String instrumentoNemo;
    private String instrumentoNombre;
    private BigDecimal ingresoCantidad;
    private BigDecimal egresoCantidad;
    private BigDecimal saldoCantidadAcumulada;
    private BigDecimal ingresoValor;
    private BigDecimal egresoValor;
    private BigDecimal utilidadPerdida;

    public TransaccionNemoDTO(java.sql.Date fechaTransaccion, String cuenta, String instrumentoNemo,
                               String instrumentoNombre, BigDecimal ingresoCantidad, BigDecimal egresoCantidad,
                               BigDecimal saldoCantidadAcumulada, BigDecimal ingresoValor, BigDecimal egresoValor,
                               BigDecimal utilidadPerdida) {
        this.fechaTransaccion = fechaTransaccion != null ? fechaTransaccion.toLocalDate() : null;
        this.cuenta = cuenta;
        this.instrumentoNemo = instrumentoNemo;
        this.instrumentoNombre = instrumentoNombre;
        this.ingresoCantidad = ingresoCantidad;
        this.egresoCantidad = egresoCantidad;
        this.saldoCantidadAcumulada = saldoCantidadAcumulada;
        this.ingresoValor = ingresoValor;
        this.egresoValor = egresoValor;
        this.utilidadPerdida = utilidadPerdida;
    }

    // Constructor con todos los argumentos para Lombok
    public TransaccionNemoDTO(LocalDate fechaTransaccion, String cuenta, String instrumentoNemo,
                               String instrumentoNombre, BigDecimal ingresoCantidad, BigDecimal egresoCantidad,
                               BigDecimal saldoCantidadAcumulada, BigDecimal ingresoValor, BigDecimal egresoValor,
                               BigDecimal utilidadPerdida) {
        this.fechaTransaccion = fechaTransaccion;
        this.cuenta = cuenta;
        this.instrumentoNemo = instrumentoNemo;
        this.instrumentoNombre = instrumentoNombre;
        this.ingresoCantidad = ingresoCantidad;
        this.egresoCantidad = egresoCantidad;
        this.saldoCantidadAcumulada = saldoCantidadAcumulada;
        this.ingresoValor = ingresoValor;
        this.egresoValor = egresoValor;
        this.utilidadPerdida = utilidadPerdida;
    }
}
