package com.costing.engine;

import com.model.entities.KardexEntity;
import com.model.entities.TransaccionEntity;
import com.model.enums.TipoEnumsCosteo;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class KardexFactory {
    
    private static final int ROUNDING_SCALE = 6;

    public KardexEntity createFromIngreso(TransaccionEntity tx, BigDecimal saldoQty, BigDecimal saldoVal, String clave) {
        KardexEntity k = new KardexEntity();
        BigDecimal costoUnitario = tx.getPrecio();
        
        k.setTransaccion(tx);
        k.setClaveAgrupacion(clave);
        k.setFechaTransaccion(tx.getFecha());
        k.setFolio(tx.getFolio());
        k.setTipoContable(TipoEnumsCosteo.INGRESO);
        k.setCantidad(tx.getCantidad());
        k.setCostoUnitario(costoUnitario);
        k.setCostoTotal(tx.getCantidad().multiply(costoUnitario));
        k.setSaldoCantidad(saldoQty);
        k.setSaldoValor(saldoVal);
        k.setEmpresa(tx.getEmpresa());
        k.setCuenta(tx.getCuenta());
        k.setCustodio(tx.getCustodio());
        k.setInstrumento(tx.getInstrumento());
        k.setCantidadDisponible(tx.getCantidad()); 
        return k;
    }

    public KardexEntity createFromEgreso(TransaccionEntity tx, BigDecimal cantidadUsada, BigDecimal costoParcial, BigDecimal saldoQty, BigDecimal saldoVal, String clave) {
        BigDecimal costoUnitario = BigDecimal.ZERO;
        if (cantidadUsada != null && cantidadUsada.compareTo(BigDecimal.ZERO) != 0) {
            costoUnitario = costoParcial.divide(cantidadUsada, ROUNDING_SCALE, RoundingMode.HALF_UP);
        }

        KardexEntity k = new KardexEntity();
        k.setTransaccion(tx);
        k.setClaveAgrupacion(clave);
        k.setFechaTransaccion(tx.getFecha());
        k.setFolio(tx.getFolio());
        k.setTipoContable(TipoEnumsCosteo.EGRESO);
        k.setCantidad(cantidadUsada);
        k.setCostoUnitario(costoUnitario);
        k.setCostoTotal(costoParcial);
        k.setSaldoCantidad(saldoQty);
        k.setSaldoValor(saldoVal);
        k.setCantidadDisponible(null); 
        k.setEmpresa(tx.getEmpresa());
        k.setCuenta(tx.getCuenta());
        k.setCustodio(tx.getCustodio());
        k.setInstrumento(tx.getInstrumento());
        return k;
    }
}