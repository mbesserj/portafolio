package com.serv.service;

import com.model.interfaces.AbstractRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servicio dedicado a limpiar las tablas de negocio antes de una carga inicial.
 * Utiliza el helper transaccional de AbstractRepository para garantizar la atomicidad.
 */
public class LimpiezaService extends AbstractRepository {

    private static final Logger logger = LoggerFactory.getLogger(LimpiezaService.class);

    /**
     * Constructor.
     */
    public LimpiezaService() {
        super();
    }

    /**
     * Vacía todas las tablas de negocio dentro de una transacción segura.
     * Garantiza que las revisiones de llaves foráneas se reactiven siempre,
     * incluso si ocurre un error durante el truncado.
     */
    public void limpiarDatosDeNegocio() {
        logger.info("Iniciando limpieza de tablas de negocio...");

        // CAMBIO 1: Toda la lógica se envuelve en el helper transaccional.
        // Esto asegura que la operación completa sea atómica.
        executeInTransaction(em -> {
            // CAMBIO 2: Se mantiene un bloque try-finally DENTRO de la transacción.
            // Esto es crucial para asegurar que las llaves foráneas se reactiven
            // sin importar si el truncado de tablas tiene éxito o falla.
            try {
                // Desactivamos temporalmente la revisión de llaves foráneas.
                logger.debug("Revisión de llaves foráneas desactivada.");
                em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0;").executeUpdate();

                // Se vacían TODAS las tablas de negocio.
                logger.debug("Truncando tablas: detalle_costeos, kardex, saldos_kardex, transacciones, saldos_diarios, saldos, carga_transacciones...");
                em.createNativeQuery("TRUNCATE TABLE detalle_costeos").executeUpdate();
                em.createNativeQuery("TRUNCATE TABLE kardex").executeUpdate();
                em.createNativeQuery("TRUNCATE TABLE saldos_kardex").executeUpdate();
                em.createNativeQuery("TRUNCATE TABLE transacciones").executeUpdate();
                em.createNativeQuery("TRUNCATE TABLE saldos_diarios").executeUpdate();
                em.createNativeQuery("TRUNCATE TABLE saldos").executeUpdate();
                em.createNativeQuery("TRUNCATE TABLE carga_transacciones").executeUpdate();

            } finally {
                // Es CRUCIAL volver a activar la revisión de llaves foráneas.
                // El bloque 'finally' garantiza que esta línea se ejecute siempre.
                logger.debug("Revisión de llaves foráneas reactivada.");
                em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1;").executeUpdate();
            }
        });

        logger.info("Limpieza de tablas completada exitosamente.");
    }
}