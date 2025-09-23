package com.app.dto;

import com.app.entities.CargaTransaccionEntity;
import com.app.utiles.Pk;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data 
@NoArgsConstructor 
public class CartolaBanChile {

    private Pk id;

    private String rut = "8713916-6"; // es necesario mejorar esto.
    
    // Sección de movimientos
    private String clienteMovimiento;
    private String cuentaMovimiento;
    private LocalDate fechaLiquidacion;
    private LocalDate fechaMovimiento;
    private String productoMovimiento;
    private String movimientoCaja;
    private String operacion;
    private String instrumentoNemo;
    private String instrumentoNombre;
    private String detalle;
    private BigDecimal cantidad;
    private String monedaOrigen;
    private BigDecimal precio;
    private BigDecimal comision;
    private BigDecimal iva;
    private BigDecimal montoTransadoMO;
    private BigDecimal montoTransadoClp;

    // Sección de saldos
    private String clienteSaldo;
    private LocalDate fechaSaldo;
    private String cuentaSaldo;
    private String productoSaldo;
    private String instrumentoSaldo;
    private String nombreSaldo;
    private String emisor;
    private String monedaOrigenSaldo;
    private BigDecimal montoInicialOrigen;
    private BigDecimal ingresoNetoOrigen;
    private BigDecimal montoFinalOrigen;
    private BigDecimal montoFinalClp;
    private BigDecimal nominalesFinal;
    private BigDecimal precioTasaSaldo;
    private BigDecimal variacionPeriodoOrigen;
    private BigDecimal rentabilidadPeriodoOrigen;

    public CargaTransaccionEntity toEntity() {
        String tipoClase = (this.id != null && this.id.getTipoClase() != null) ? this.id.getTipoClase() : "T";
        LocalDate fecha = id.getTransactionDate() != null ? id.getTransactionDate() : (this.id != null ? this.id.getTransactionDate() : null);
        int rowNum = (this.id != null) ? this.id.getRowNum() : 0;

        CargaTransaccionEntity entity = new CargaTransaccionEntity();
        entity.setId(new Pk(fecha, rowNum, tipoClase));

        if ("S".equals(tipoClase)) {
            entity.setRazonSocial(this.clienteSaldo);
            entity.setCuenta(this.cuentaSaldo);
            entity.setProducto(this.productoSaldo);
            entity.setInstrumentoNemo(this.instrumentoSaldo);
            entity.setInstrumentoNombre(this.nombreSaldo);
            entity.setMoneda(this.monedaOrigenSaldo);
            entity.setCantidad(this.nominalesFinal);
            entity.setPrecio(this.precioTasaSaldo);
            entity.setMonto(this.montoFinalOrigen);
            entity.setMontoTotal(null);
            entity.setMontoClp(this.montoFinalClp);
            entity.setMovimientoCaja("Saldo");
            entity.setTipoMovimiento("Saldo");
        } else {
            entity.setRazonSocial(this.clienteMovimiento);
            entity.setCuenta(this.cuentaMovimiento);
            entity.setProducto(this.productoMovimiento);
            entity.setInstrumentoNemo(this.instrumentoNemo);
            entity.setInstrumentoNombre(this.instrumentoNombre);
            entity.setMoneda(this.monedaOrigen);
            entity.setCantidad(this.cantidad);
            entity.setPrecio(this.precio);
            entity.setComisiones(this.comision);
            entity.setIva(this.iva);
            entity.setMonto(this.montoTransadoClp);
            entity.setMontoTotal(this.montoTransadoClp);
            entity.setMontoClp(this.montoTransadoClp);
            entity.setMovimientoCaja(this.movimientoCaja);
            entity.setTipoMovimiento(this.operacion);
        }

        entity.setCustodioNombre("BanChile");
        entity.setRut(this.rut);
        entity.setFolio(null);

        return entity;
    }
}