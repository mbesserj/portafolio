package com.app.dto;

import com.app.entities.CargaTransaccionEntity;
import com.app.utiles.Pk;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO genérico para la carga de transacciones. Esta clase sirve como un
 * contenedor de datos intermedio que puede ser mapeado desde varios tipos de
 * archivos de origen (MovimientoTitulo, MovimientoCaja, Stock).
 */
@Data 
@NoArgsConstructor 
public class CargaTransaccion {

    private LocalDate transactionDate;
    private int rowNum;
    private String tipoClase;

    // Campos comunes a todos los archivos
    private String razonSocial;
    private String rut;
    private String cuenta;
    private String custodioNombre;
    private String tipoMovimiento;
    private String movimientoCaja;
    private String folio;
    private String producto;
    private String instrumentoNemo;
    private String instrumentoNombre;
    private String moneda;
    private BigDecimal cantidad;
    private BigDecimal precio;
    private BigDecimal monto;
    private BigDecimal montoTotal;
    private BigDecimal comisiones;
    private BigDecimal gastos;
    private BigDecimal iva;

    // Campos específicos de Stock
    private String cuentaPsh;
    private BigDecimal cantLibre;
    private BigDecimal cantGarantia;
    private BigDecimal cantPlazo;
    private BigDecimal cantVc;
    private BigDecimal cantTotal;
    private BigDecimal montoClp;
    private BigDecimal montoUsd;


    public CargaTransaccion(
            LocalDate transactionDate,
            int rowNum,
            String tipoClase,
            String razonSocial,
            String rut,
            String custodioNombre,
            String cuenta,
            String cuentaPsh,
            String instrumentoNemo,
            String instrumentoNombre,
            String tipoMovimiento,
            BigDecimal monto,
            BigDecimal montoTotal,
            String moneda,
            String movimientoCaja,
            BigDecimal montoClp,
            BigDecimal montoUsd,
            BigDecimal cantidad,
            BigDecimal precio,
            BigDecimal comisiones,
            BigDecimal gastos,
            BigDecimal iva,
            String folio) {
        this.transactionDate = transactionDate;
        this.rowNum = rowNum;
        this.tipoClase = tipoClase;
        this.razonSocial = razonSocial;
        this.rut = rut;
        this.custodioNombre = custodioNombre;
        this.cuenta = cuenta;
        this.cuentaPsh = cuentaPsh;
        this.instrumentoNemo = instrumentoNemo;
        this.instrumentoNombre = instrumentoNombre;
        this.tipoMovimiento = tipoMovimiento;
        this.monto = monto;
        this.montoTotal = montoTotal;
        this.moneda = moneda;
        this.movimientoCaja = movimientoCaja;
        this.montoClp = montoClp;
        this.montoUsd = montoUsd;
        this.cantidad = cantidad;
        this.precio = precio;
        this.comisiones = comisiones;
        this.gastos = gastos;
        this.iva = iva;
        this.folio = folio;
    }


    /**
     * Mapea el DTO a la entidad de la base de datos.
     *
     * @return Una nueva instancia de CargaTransaccionEntity.
     */
    public CargaTransaccionEntity toEntity() {
        CargaTransaccionEntity entity = new CargaTransaccionEntity();

        // Clave primaria compuesta: se construye siempre
        entity.setId(new Pk(transactionDate, rowNum, tipoClase));

        // Campos comunes
        entity.setRazonSocial(razonSocial != null ? razonSocial : "");
        entity.setRut(rut != null ? rut : "");
        entity.setCuenta(cuenta != null ? cuenta : "");
        entity.setCustodioNombre(custodioNombre != null ? custodioNombre : "");
        entity.setFolio(folio != null ? folio : "");
        entity.setInstrumentoNemo(instrumentoNemo != null ? instrumentoNemo : "");
        entity.setInstrumentoNombre(instrumentoNombre != null ? instrumentoNombre : "");
        entity.setMoneda(moneda != null ? moneda : "");
        entity.setTipoMovimiento(tipoMovimiento != null ? tipoMovimiento : "");
        entity.setMovimientoCaja(movimientoCaja != null ? movimientoCaja : "");
        entity.setProducto(producto != null ? producto : "");

        // Campos numéricos
        entity.setCantidad(cantidad != null ? cantidad : BigDecimal.ZERO);
        entity.setPrecio(precio != null ? precio : BigDecimal.ZERO);
        entity.setMonto(monto != null ? monto : BigDecimal.ZERO);
        entity.setMontoTotal(montoTotal != null ? montoTotal : BigDecimal.ZERO);
        entity.setComisiones(comisiones != null ? comisiones : BigDecimal.ZERO);
        entity.setGastos(gastos != null ? gastos : BigDecimal.ZERO);
        entity.setIva(iva != null ? iva : BigDecimal.ZERO);

        // Campos específicos de Stock
        entity.setCuentaPsh(cuentaPsh != null ? cuentaPsh : "");
        entity.setCantLibre(cantLibre != null ? cantLibre : BigDecimal.ZERO);
        entity.setCantGarantia(cantGarantia != null ? cantGarantia : BigDecimal.ZERO);
        entity.setCantPlazo(cantPlazo != null ? cantPlazo : BigDecimal.ZERO);
        entity.setCantVc(cantVc != null ? cantVc : BigDecimal.ZERO);
        entity.setCantTotal(cantTotal != null ? cantTotal : BigDecimal.ZERO);
        entity.setMontoClp(montoClp != null ? montoClp : BigDecimal.ZERO);
        entity.setMontoUsd(montoUsd != null ? montoUsd : BigDecimal.ZERO);

        return entity;
    }

    /**
     * 
     * @param entity
     * @return retorna una Dto a partir de una Entity
     */
    public static CargaTransaccion fromEntity(CargaTransaccionEntity entity) {
        CargaTransaccion dto = new CargaTransaccion();

        dto.setTransactionDate(entity.getId().getTransactionDate());
        dto.setRowNum(entity.getId().getRowNum());
        dto.setTipoClase(entity.getId().getTipoClase());
        dto.setRazonSocial(entity.getRazonSocial());
        dto.setRut(entity.getRut());
        dto.setCuenta(entity.getCuenta());
        dto.setCuentaPsh(entity.getCuentaPsh());
        dto.setCustodioNombre(entity.getCustodioNombre());
        dto.setTipoMovimiento(entity.getTipoMovimiento());
        dto.setFolio(entity.getFolio());
        dto.setInstrumentoNemo(entity.getInstrumentoNemo());
        dto.setInstrumentoNombre(entity.getInstrumentoNombre());
        dto.setCantidad(entity.getCantidad());
        dto.setPrecio(entity.getPrecio());
        dto.setMonto(entity.getMonto());
        dto.setComisiones(entity.getComisiones());
        dto.setGastos(entity.getGastos());
        dto.setMontoTotal(entity.getMontoTotal());
        dto.setMoneda(entity.getMoneda());
        dto.setCantLibre(entity.getCantLibre());
        dto.setCantGarantia(entity.getCantGarantia());
        dto.setCantPlazo(entity.getCantPlazo());
        dto.setCantVc(entity.getCantVc());
        dto.setCantTotal(entity.getCantTotal());
        dto.setMontoClp(entity.getMontoClp());
        dto.setMontoUsd(entity.getMontoUsd());
        dto.setIva(entity.getIva());

        return dto;
    }
}