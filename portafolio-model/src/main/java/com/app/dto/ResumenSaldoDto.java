
package com.app.dto;

import java.math.BigDecimal;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResumenSaldoDto {
    private Long instrumentoId;
    private String instrumentoNemo;
    private String instrumentoNombre;
    private Long empresaId;
    private Long custodioId;
    private BigDecimal saldoCantidad;
    private BigDecimal costoTotal;
    private BigDecimal costoUnitario;
    private BigDecimal cantidadMercado;
    private BigDecimal precioMercado;
    private BigDecimal valorMercado;
    private BigDecimal utilidadNoRealizada;
}