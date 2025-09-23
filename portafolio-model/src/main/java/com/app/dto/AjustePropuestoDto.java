package com.app.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AjustePropuestoDto {

    private final LocalDate fecha;
    private final String tipo;
    private final BigDecimal cantidad;
    private final BigDecimal precio;
    private final String observacion;
    private BigDecimal saldoAnteriorCantidad;
    private LocalDate saldoAnteriorFecha;

    public AjustePropuestoDto(LocalDate fecha, String tipo, BigDecimal cantidad, BigDecimal precio, String observacion) {
        this.fecha = fecha;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.precio = precio;
        this.observacion = observacion;
    }

    // Getters para acceder a los valores
    
    public LocalDate getFecha() {
        return fecha;
    }

    public String getTipo() {
        return tipo;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public String getObservacion() {
        return observacion;
    }
    
    public BigDecimal getSaldoAnteriorCantidad() { 
        return saldoAnteriorCantidad; 
    }
    
    public void setSaldoAnteriorCantidad(BigDecimal saldoAnteriorCantidad) { 
        this.saldoAnteriorCantidad = saldoAnteriorCantidad; 
    }
    public LocalDate getSaldoAnteriorFecha() {
        return saldoAnteriorFecha; 
    }
    
    public void setSaldoAnteriorFecha(LocalDate saldoAnteriorFecha) {
        this.saldoAnteriorFecha = saldoAnteriorFecha; 
    }
}