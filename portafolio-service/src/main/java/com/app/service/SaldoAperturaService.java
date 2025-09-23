package com.app.service;

import com.app.entities.SaldoEntity;
import com.app.entities.TipoMovimientoEntity;
import com.app.entities.TransaccionEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servicio encargado de crear los registros iniciales en la tabla 'saldos'
 * a partir de las transacciones de apertura.
 */
public class SaldoAperturaService {

    private static final Logger logger = LoggerFactory.getLogger(SaldoAperturaService.class);
    private final EntityManager em;

    public SaldoAperturaService(EntityManager em) {
        this.em = em;
    }

    /**
     * Lee las transacciones de tipo 'SALDO INICIAL' y crea
     * un registro en la tabla 'saldos' para cada una, copiando todos los
     * campos relevantes.
     */
    public void crearSaldosDeAperturaDesdeTransacciones() {
        logger.info("Iniciando la creación de saldos de apertura desde transacciones.");

        TipoMovimientoEntity tipoMovimientoSaldoInicial;
        try {
            tipoMovimientoSaldoInicial = em.createQuery(
                "SELECT tm FROM TipoMovimientoEntity tm WHERE tm.tipoMovimiento = 'SALDO INICIAL'",
                TipoMovimientoEntity.class
            ).getSingleResult();
        } catch (NoResultException e) {
            logger.error("No se encontró el TipoMovimiento 'SALDO INICIAL'. No se pueden crear los saldos de apertura.");
            throw new IllegalStateException("El tipo de movimiento 'SALDO INICIAL' debe existir para la carga inicial.");
        }
        
        List<TransaccionEntity> transaccionesDeApertura = em.createQuery(
            "SELECT t FROM TransaccionEntity t WHERE t.tipoMovimiento = :tipoMov",
            TransaccionEntity.class
        ).setParameter("tipoMov", tipoMovimientoSaldoInicial).getResultList();
        
        if (transaccionesDeApertura.isEmpty()) {
            logger.warn("No se encontraron transacciones de tipo 'SALDO INICIAL' para procesar.");
            return;
        }

        logger.info("Se encontraron {} transacciones de apertura. Creando registros en la tabla 'saldos'...", transaccionesDeApertura.size());

        for (TransaccionEntity tx : transaccionesDeApertura) {
            SaldoEntity nuevoSaldo = new SaldoEntity();
            
            // --- CAMPOS DE RELACIÓN Y BÁSICOS ---
            nuevoSaldo.setEmpresa(tx.getEmpresa());
            nuevoSaldo.setCustodio(tx.getCustodio());
            nuevoSaldo.setInstrumento(tx.getInstrumento());
            nuevoSaldo.setCuenta(tx.getCuenta());
            nuevoSaldo.setFecha(tx.getFecha());
            
            // --- CAMPOS DE CANTIDADES Y VALORES ---
            nuevoSaldo.setCantidad(tx.getCantidad());
            nuevoSaldo.setCantGarantia(BigDecimal.ZERO);
            nuevoSaldo.setCantLibre(BigDecimal.ZERO);
            nuevoSaldo.setCantPlazo(BigDecimal.ZERO);
            nuevoSaldo.setCantVc(BigDecimal.ZERO);
            nuevoSaldo.setPrecio(tx.getPrecio());
            nuevoSaldo.setMontoClp(tx.getMontoClp());
            nuevoSaldo.setMontoUsd(BigDecimal.ZERO);
            nuevoSaldo.setMoneda(tx.getMoneda());

            nuevoSaldo.setCreadoPor("CARGA_INICIAL");
            nuevoSaldo.setFechaCreacion(LocalDate.now());

            em.persist(nuevoSaldo);
        }
        
        logger.info("Proceso de creación de saldos de apertura finalizado con éxito.");
    }
}