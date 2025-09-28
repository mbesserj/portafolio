package com.costing.engine;

import com.model.entities.KardexEntity;
import java.math.BigDecimal;

/**
 * Representa un lote de ingreso en la cola FIFO con su cantidad restante.
 */
public class IngresoDisponible {
    KardexEntity kardexIngreso;
    BigDecimal cantidadDisponible;

    public IngresoDisponible(KardexEntity kardexIngreso) {
        this.kardexIngreso = kardexIngreso;
        this.cantidadDisponible = kardexIngreso.getCantidadDisponible();
    }
}