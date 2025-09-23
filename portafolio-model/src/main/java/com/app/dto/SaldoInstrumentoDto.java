package com.app.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class SaldoInstrumentoDto {

    private Long instrumentoId;
    private String instrumentoNemo;
    private String nombreInstrumento;
    private String cuenta;
    
    // --- Datos de Costo (vienen del Kárdex) ---
    private BigDecimal saldoDisponible;
    private BigDecimal costoUnitarioSaldo;
    private BigDecimal costoTotalSaldo; 
    
    // --- Datos de Mercado (vienen de SaldoEntity) ---
    private BigDecimal precioMercado;
    private BigDecimal valorMercado;
    
    // --- Campo Calculado ---
    private BigDecimal utilidadNoRealizada;

}