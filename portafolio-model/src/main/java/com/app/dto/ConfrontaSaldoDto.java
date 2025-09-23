package com.app.dto;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.SqlResultSetMapping;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para mapear el resultado de la consulta SQL nativa que confronta
 * y agrupa los saldos de Kardex y la tabla de Saldos.
 */
@SqlResultSetMapping(
    name = "ConfrontaSaldoMapping",
    classes = @ConstructorResult(
        targetClass = ConfrontaSaldoDto.class,
        columns = {
            // Columnas visibles en la tabla
            @ColumnResult(name = "empresa_nombre", type = String.class),
            @ColumnResult(name = "custodio_nombre", type = String.class),
            @ColumnResult(name = "instrumento_nemo", type = String.class),
            @ColumnResult(name = "cuenta", type = String.class),
            @ColumnResult(name = "ultima_fecha_kardex", type = LocalDate.class),
            @ColumnResult(name = "cantidad_kardex", type = BigDecimal.class),
            @ColumnResult(name = "valor_kardex", type = BigDecimal.class),
            @ColumnResult(name = "ultima_fecha_saldos", type = LocalDate.class),
            @ColumnResult(name = "cantidad_mercado", type = BigDecimal.class),
            @ColumnResult(name = "valor_mercado", type = BigDecimal.class),
            @ColumnResult(name = "diferencia_cantidad", type = BigDecimal.class),
            
            // Columnas adicionales para la lógica de negocio (ajustes)
            @ColumnResult(name = "empresa_id", type = Long.class),
            @ColumnResult(name = "custodio_id", type = Long.class),
            @ColumnResult(name = "instrumento_id", type = Long.class),
            @ColumnResult(name = "precio_mercado", type = BigDecimal.class)
        }
    )
)
@Data
@NoArgsConstructor
public class ConfrontaSaldoDto {
    // Campos visibles
    private String empresaNombre;
    private String custodioNombre;
    private String instrumentoNemo;
    private String cuenta;
    private LocalDate ultimaFechaKardex;
    private BigDecimal cantidadKardex;
    private BigDecimal valorKardex;
    private LocalDate ultimaFechaSaldos;
    private BigDecimal cantidadMercado;
    private BigDecimal valorMercado;
    private BigDecimal diferenciaCantidad;

    // Campos para la lógica de negocio
    private Long empresaId;
    private Long custodioId;
    private Long instrumentoId;
    private BigDecimal precioMercado;

    /**
     * Constructor utilizado por el SqlResultSetMapping.
     * El orden y tipo de los parámetros DEBE coincidir con el de los @ColumnResult.
     */
    public ConfrontaSaldoDto(String empresaNombre, String custodioNombre, String instrumentoNemo, String cuenta,
                             LocalDate ultimaFechaKardex, BigDecimal cantidadKardex, BigDecimal valorKardex,
                             LocalDate ultimaFechaSaldos, BigDecimal cantidadMercado, BigDecimal valorMercado,
                             BigDecimal diferenciaCantidad, Long empresaId, Long custodioId,
                             Long instrumentoId, BigDecimal precioMercado) {
        this.empresaNombre = empresaNombre;
        this.custodioNombre = custodioNombre;
        this.instrumentoNemo = instrumentoNemo;
        this.cuenta = cuenta;
        this.ultimaFechaKardex = ultimaFechaKardex;
        this.cantidadKardex = cantidadKardex;
        this.valorKardex = valorKardex;
        this.ultimaFechaSaldos = ultimaFechaSaldos;
        this.cantidadMercado = cantidadMercado;
        this.valorMercado = valorMercado;
        this.diferenciaCantidad = diferenciaCantidad;
        this.empresaId = empresaId;
        this.custodioId = custodioId;
        this.instrumentoId = instrumentoId;
        this.precioMercado = precioMercado;
    }
}
