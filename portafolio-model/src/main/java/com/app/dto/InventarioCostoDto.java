package com.app.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventarioCostoDto {
    private Long instrumentoId;
    private String instrumentoNemo;
    private String instrumentoNombre;
    private BigDecimal saldoCantidadFinal;
    private BigDecimal costoTotalFifo;
}