package com.app.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProblemasTrxsDto {

    private LocalDate fecha;
    private String folio;
    private String tipoMovimiento;
    private String instrumentoNemo;
    private BigDecimal compras;
    private BigDecimal ventas;
    private BigDecimal precio;
    private BigDecimal total;
    private boolean costeado;
    
    // CONSTRUCTOR REQUERIDO POR LA CONSULTA HQL
    public ProblemasTrxsDto(
        LocalDate fecha,
        String folio,
        String tipoMovimiento,
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
        this.instrumentoNemo = instrumentoNemo;
        this.compras = compras;
        this.ventas = ventas;
        this.precio = precio;
        this.total = total;
        this.costeado = costeado;
    }
}
