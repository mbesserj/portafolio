package com.app.dto;

import com.app.enums.TipoEnumsCosteo;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OperacionesTrxsDto {

    private Long id;
    private LocalDate fecha;
    private String folio;
    private String tipoMovimiento;
    private TipoEnumsCosteo tipoContable;
    private BigDecimal compras;
    private BigDecimal ventas;
    private BigDecimal saldoAcumulado;
    private BigDecimal precio;
    private BigDecimal total;
    private boolean costeado;
    private boolean paraRevision;
    private boolean ignorarEnCosteo;

    public OperacionesTrxsDto(
        Long id,
        LocalDate fecha,
        String folio,
        String tipoMovimiento,
        TipoEnumsCosteo tipoContable,
        BigDecimal compras,
        BigDecimal ventas,        
        BigDecimal precio,
        BigDecimal total,
        boolean costeado,
        boolean paraRevision,
        boolean ignorarEnCosteo
    ) {
        this.id = id;
        this.fecha = fecha;
        this.folio = folio;
        this.tipoMovimiento = tipoMovimiento;
        this.tipoContable = tipoContable;
        this.compras = compras;
        this.ventas = ventas;
        this.precio = precio;
        this.total = total;
        this.costeado = costeado;
        this.paraRevision = paraRevision;
        this.ignorarEnCosteo = ignorarEnCosteo;
    }

    public OperacionesTrxsDto(BigDecimal sumCompras, BigDecimal sumVentas, BigDecimal sumTotal) {
        this.compras = sumCompras;
        this.ventas = sumVentas;
        this.total = sumTotal;
    }
}