
package com.app.engine;

import com.app.entities.KardexEntity;
import com.app.entities.TransaccionEntity;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.Queue;

public class IngresoHandler {
    private final EntityManager em;
    private final KardexFactory kardexFactory = new KardexFactory();

    public IngresoHandler(EntityManager em) { this.em = em; }

    public BigDecimal handle(TransaccionEntity tx, Queue<IngresoDisponible> queue, BigDecimal currentQty, BigDecimal currentVal, String clave) {
        BigDecimal newQty = currentQty.add(tx.getCantidad());
        BigDecimal newVal = currentVal.add(tx.getCantidad().multiply(tx.getPrecio()));

        KardexEntity kardex = kardexFactory.createFromIngreso(tx, newQty, newVal, clave);
        em.persist(kardex);
        queue.add(new IngresoDisponible(kardex));
        
        return newQty;
    }
}