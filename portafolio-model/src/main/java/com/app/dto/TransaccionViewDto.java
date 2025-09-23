package com.app.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TransaccionViewDto {

    private LocalDate fecha;
    private String folio;
    private String tipoMovimiento;
    private String tipoContable;
    private String instrumentoNemo;
    private BigDecimal compras;
    private BigDecimal ventas;
    private BigDecimal precio;
    private BigDecimal total;
    private boolean costeado;

    // Constructor unificado con 10 parámetros para ambas consultas
    public TransaccionViewDto(
        LocalDate fecha,
        String folio,
        String tipoMovimiento,
        String tipoContable, 
        String instrumentoNemo,
        BigDecimal compras,
        BigDecimal ventas,
        BigDecimal precio,
        BigDecimal total,
        boolean costeado
    ) {
        this.fecha = fecha;
        this.folio = folio;
        this.tipoMovimiento = tipoMovimiento;
        this.tipoContable = tipoContable;
        this.instrumentoNemo = instrumentoNemo;
        this.compras = compras;
        this.ventas = ventas;
        this.precio = precio;
        this.total = total;
        this.costeado = costeado;
    }
}