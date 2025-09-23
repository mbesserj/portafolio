package com.app.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ResultadoInstrumentoDto {

    private Long idTransaccion; 
    private LocalDate fecha;
    private String tipoMovimiento;
    private BigDecimal cantidad;
    private BigDecimal cant_compra;
    private BigDecimal cant_venta;
    private BigDecimal saldo;
    private BigDecimal compras;
    private BigDecimal ventas;
    private BigDecimal dividendos;
    private BigDecimal gastos;
    private BigDecimal utilidadRealizada;
    private BigDecimal costoDeVenta;

    /**
     * Este es el constructor que el servicio necesita para los datos de la VIEW.
     */
    public ResultadoInstrumentoDto(Long idTransaccion, LocalDate fecha, String tipoMovimiento, BigDecimal cantidad) {
        this.idTransaccion = idTransaccion;
        this.fecha = fecha;
        this.tipoMovimiento = tipoMovimiento;
        this.cantidad = cantidad;
    }

    /**
     * Constructor simple para crear filas especiales como "TOTALES".
     */
    public ResultadoInstrumentoDto(String tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }

}