package com.app.dto;

import com.app.entities.CargaTransaccionEntity;
import com.app.utiles.Pk;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor 
public class CartolaFynsa {

    private Pk id;
    private String tipoClase;
    private Integer rowNum;

    // Comunes
    private LocalDate transactionDate;
    private String razonSocial;
    private String rut;
    private String cuenta;
    private String custodio;
    private String instrumentoNemo;
    private String instrumentoNombre;
    private String moneda;

    // Movimiento
    private String tipoMovimiento;
    private String folio;
    private BigDecimal cantidad;
    private BigDecimal precio;
    private BigDecimal monto;
    private BigDecimal comisiones;
    private BigDecimal gastos;
    private BigDecimal montoTotal;

    // Stock
    private String cuentaPsh;
    private BigDecimal cantLibre;
    private BigDecimal cantGarantia;
    private BigDecimal cantPlazo;
    private BigDecimal cantVc;
    private BigDecimal cantTotal;
    private BigDecimal montoClp;
    private BigDecimal montoUsd;

    public CargaTransaccionEntity toEntity() { 
        CargaTransaccionEntity entity = new CargaTransaccionEntity();
        entity.setId(new Pk(id.getTransactionDate(), id.getRowNum(), id.getTipoClase()));

        entity.setRazonSocial(this.razonSocial);
        entity.setRut(this.rut);
        entity.setCuenta(this.cuenta);
        entity.setProducto(null);
        entity.setInstrumentoNemo(this.instrumentoNemo);
        entity.setInstrumentoNombre(this.instrumentoNombre);
        entity.setMoneda(this.moneda);
        entity.setCustodioNombre(this.custodio);
        entity.setFolio(this.folio);
        entity.setTipoMovimiento(this.tipoMovimiento);

        switch (tipoClase) {
            case "C":
                entity.setCantidad(this.cantidad);
                entity.setPrecio(this.precio);
                entity.setMonto(this.monto);
                entity.setMontoTotal(this.montoTotal);
                break;
            case "T":
                entity.setCantidad(this.cantidad);
                entity.setPrecio(this.precio);
                entity.setMonto(this.monto);
                entity.setComisiones(this.comisiones);
                entity.setGastos(this.gastos);
                entity.setMontoTotal(this.montoTotal);
                break;
            case "S":
                entity.setCantidad(this.cantTotal);
                entity.setPrecio(this.precio);
                entity.setMontoUsd(this.montoUsd);
                entity.setMontoClp(this.montoClp);
                entity.setMontoTotal(null);
                break;
        }
        return entity;
    }

    public CargaTransaccion toDto(LocalDate transactionDate, Integer rowNum, String tipoClase) {
        CargaTransaccion dto = new CargaTransaccion();

        dto.setTransactionDate(transactionDate);
        dto.setRowNum(rowNum);
        dto.setTipoClase(tipoClase);

        dto.setRazonSocial(this.razonSocial);
        dto.setRut(this.rut);
        dto.setCuenta(this.cuenta);
        dto.setProducto(null); // o cuentaPsh si aplica
        dto.setInstrumentoNemo(this.instrumentoNemo);
        dto.setInstrumentoNombre(this.instrumentoNombre);
        dto.setMoneda(this.moneda);
        dto.setCustodioNombre(this.custodio);
        dto.setFolio(this.folio);
        dto.setTipoMovimiento(this.tipoMovimiento);

        switch (tipoClase) {
            case "C":
                dto.setCantidad(this.cantidad);
                dto.setPrecio(this.precio);
                dto.setMonto(this.monto);
                break;
            case "T":
                dto.setCantidad(this.cantidad);
                dto.setPrecio(this.precio);
                dto.setMonto(this.monto);
                dto.setComisiones(this.comisiones);
                dto.setGastos(this.gastos);
                dto.setMontoTotal(this.montoTotal);
                break;
            case "S":
                dto.setCantLibre(this.cantLibre);
                dto.setCantGarantia(this.cantGarantia);
                dto.setCantPlazo(this.cantPlazo);
                dto.setCantVc(this.cantVc);
                dto.setCuentaPsh(this.cuentaPsh);
                dto.setCantidad(this.cantTotal);
                dto.setPrecio(this.precio);
                dto.setMonto(this.montoUsd);
                dto.setMontoClp(this.montoClp);
                dto.setMontoUsd(this.montoUsd);
                break;
        }
        return dto;
    }
}