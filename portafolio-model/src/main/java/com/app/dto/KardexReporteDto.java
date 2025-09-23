package com.app.dto;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@SqlResultSetMapping(
    name = "KardexReporteMapping",
    classes = @ConstructorResult(
        targetClass = KardexReporteDto.class,
        columns = {
            // Nombres de columna actualizados para coincidir con los alias del SQL
            @ColumnResult(name = "fecha_tran", type = Date.class),
            @ColumnResult(name = "tipo_oper", type = String.class),
            @ColumnResult(name = "nemo", type = String.class),
            @ColumnResult(name = "cant_compra", type = BigDecimal.class),
            @ColumnResult(name = "precio_compra", type = BigDecimal.class),
            @ColumnResult(name = "monto_compra", type = BigDecimal.class),
            @ColumnResult(name = "total_fact", type = BigDecimal.class),
            @ColumnResult(name = "cant_usada", type = BigDecimal.class),
            @ColumnResult(name = "fecha_compra", type = Date.class),
            @ColumnResult(name = "costo_fifo", type = BigDecimal.class),
            @ColumnResult(name = "precio_venta", type = BigDecimal.class),
            @ColumnResult(name = "costo_oper", type = BigDecimal.class),
            @ColumnResult(name = "margen", type = BigDecimal.class),
            @ColumnResult(name = "utilidad", type = BigDecimal.class),
            @ColumnResult(name = "saldo_cantidad", type = BigDecimal.class),
            @ColumnResult(name = "saldo_valor", type = BigDecimal.class)
        }
    )
)
@Data
@NoArgsConstructor
public class KardexReporteDto {

    private LocalDate fechaTran;
    private String tipoOper;
    private String nemo;
    private BigDecimal cantCompra;
    private BigDecimal precioCompra;
    private BigDecimal montoCompra;
    private BigDecimal totalFact;
    private BigDecimal cantUsada;
    private LocalDate fechaCompra;
    private BigDecimal costoFifo;
    private BigDecimal precioVenta;
    private BigDecimal costoOper;
    private BigDecimal margen;
    private BigDecimal utilidad;
    private BigDecimal saldoCantidad;
    private BigDecimal saldoValor;

    public KardexReporteDto(Date fechaTran, String tipoOper, String nemo, BigDecimal cantCompra,
                            BigDecimal precioCompra, BigDecimal montoCompra, BigDecimal totalFact, BigDecimal cantUsada,
                            Date fechaCompra, BigDecimal costoFifo, BigDecimal precioVenta,
                            BigDecimal costoOper, BigDecimal margen, BigDecimal utilidad,
                            BigDecimal saldoCantidad, BigDecimal saldoValor) {
        this.fechaTran = (fechaTran != null) ? new java.sql.Date(fechaTran.getTime()).toLocalDate() : null;
        this.tipoOper = tipoOper;
        this.nemo = nemo;
        this.cantCompra = cantCompra;
        this.precioCompra = precioCompra;
        this.montoCompra = montoCompra;
        this.totalFact = totalFact;
        this.cantUsada = cantUsada;
        this.fechaCompra = (fechaCompra != null) ? new java.sql.Date(fechaCompra.getTime()).toLocalDate() : null;
        this.costoFifo = costoFifo;
        this.precioVenta = precioVenta;
        this.costoOper = costoOper;
        this.margen = margen;
        this.utilidad = utilidad;
        this.saldoCantidad = saldoCantidad;
        this.saldoValor = saldoValor;
    }
}