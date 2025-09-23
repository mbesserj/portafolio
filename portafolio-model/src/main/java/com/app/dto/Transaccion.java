package com.app.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor 
public class Transaccion {

    private LocalDate transactionDate;
    private int rowNum;
    private String fileOrigin;

    private LocalDate fecha;
    private String razonSocial;
    private String rut;
    private String folio;
    private String cuenta;
    private String instrumentoNemo;
    private String instrumentoNombre;
    private BigDecimal cantidad;
    private BigDecimal precio;
    private BigDecimal comision;
    private BigDecimal gastos;
    private BigDecimal iva;
    private BigDecimal monto;
    private BigDecimal montoTotal;
    private String moneda;
    private boolean costeado;
    private LocalDate fechaCreado;
    private String custodio;
    private String operacion;

}