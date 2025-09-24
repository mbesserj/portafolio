package com.app.engine;

import com.app.entities.KardexEntity;
import com.app.entities.TransaccionEntity;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.util.Queue;

public class IngresoHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(IngresoHandler.class);
    private final EntityManager em;
    private final KardexFactory kardexFactory = new KardexFactory();

    public IngresoHandler(EntityManager em) { 
        this.em = em; 
    }

    /**
     * Resultado del procesamiento de un ingreso
     */
    public record IngresoResult(BigDecimal nuevoSaldoCantidad, BigDecimal nuevoSaldoValor) {}

    /**
     * Procesa un ingreso y retorna los nuevos saldos
     */
    public IngresoResult handle(TransaccionEntity tx, Queue<IngresoDisponible> queue, 
                               BigDecimal currentQty, BigDecimal currentVal, String clave) {
        
        // Validaciones de entrada
        validateInputs(tx, queue, currentQty, currentVal, clave);
        
        BigDecimal newQty = currentQty.add(tx.getCantidad());
        BigDecimal valorIngreso = tx.getCantidad().multiply(tx.getPrecio());
        BigDecimal newVal = currentVal.add(valorIngreso);

        logger.debug("Procesando ingreso - Tx ID: {}, Cantidad: {}, Precio: {}, Valor: {}", 
                    tx.getId(), tx.getCantidad(), tx.getPrecio(), valorIngreso);

        try {
            KardexEntity kardex = kardexFactory.createFromIngreso(tx, newQty, newVal, clave);
            em.persist(kardex);
            queue.add(new IngresoDisponible(kardex));
            
            logger.info("Ingreso procesado exitosamente - Tx ID: {}, Nuevo saldo: qty={}, val={}", 
                       tx.getId(), newQty, newVal);
            
            return new IngresoResult(newQty, newVal);
            
        } catch (Exception e) {
            logger.error("Error al procesar ingreso - Tx ID: {}", tx.getId(), e);
            throw new RuntimeException("Error en el procesamiento del ingreso: " + e.getMessage(), e);
        }
    }
    
    private void validateInputs(TransaccionEntity tx, Queue<IngresoDisponible> queue, 
                               BigDecimal currentQty, BigDecimal currentVal, String clave) {
        if (tx == null) {
            throw new IllegalArgumentException("La transacción no puede ser nula");
        }
        if (queue == null) {
            throw new IllegalArgumentException("La cola de ingresos no puede ser nula");
        }
        if (currentQty == null || currentQty.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("La cantidad actual debe ser mayor o igual a cero");
        }
        if (currentVal == null) {
            throw new IllegalArgumentException("El valor actual no puede ser nulo");
        }
        if (clave == null || clave.trim().isEmpty()) {
            throw new IllegalArgumentException("La clave de agrupación no puede ser nula o vacía");
        }
        if (tx.getCantidad() == null || tx.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad del ingreso debe ser mayor a cero");
        }
        if (tx.getPrecio() == null || tx.getPrecio().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio del ingreso no puede ser negativo");
        }
    }
}