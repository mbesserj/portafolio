package com.app.dto;

import java.math.BigDecimal;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResumenInstrumentoDto {

    private Long instrumentoId;
    private String nemo;
    private String nombreInstrumento;
    private BigDecimal saldoDisponible;
    private BigDecimal costoFifo;
    private BigDecimal valorDeMercado;
    private BigDecimal totalDividendos;
    private BigDecimal totalGastos;
    private BigDecimal utilidadRealizada;
    private BigDecimal utilidadNoRealizada;
    private BigDecimal rentabilidad;
    private BigDecimal totalCompras; 
}