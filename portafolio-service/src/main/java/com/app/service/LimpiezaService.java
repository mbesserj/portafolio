package com.app.service;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servicio dedicado a limpiar las tablas de negocio antes de una carga inicial.
 */
public class LimpiezaService {

    private static final Logger logger = LoggerFactory.getLogger(LimpiezaService.class);
    private final EntityManager em;

    public LimpiezaService(EntityManager em) {
        this.em = em;
    }

    public void limpiarDatosDeNegocio() {
        logger.info("Iniciando limpieza de tablas de negocio...");

        try {
            // Desactivamos temporalmente la revisión de llaves foráneas.
            // Esto es seguro en este contexto porque vamos a vaciar todas las tablas relacionadas.
            em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0;").executeUpdate();
            logger.debug("Revisión de llaves foráneas desactivada.");

            // Se vacían TODAS las tablas de negocio.
            // El orden aquí ya no es tan crítico gracias a la desactivación de las llaves foráneas,
            // pero es buena práctica mantener un orden lógico.
            logger.debug("Truncando tablas: detalle_costeos, kardex, saldos_kardex, transacciones, saldos_diarios, saldos, carga_transacciones...");
            
            em.createNativeQuery("TRUNCATE TABLE detalle_costeos").executeUpdate();
            em.createNativeQuery("TRUNCATE TABLE kardex").executeUpdate();
            em.createNativeQuery("TRUNCATE TABLE saldos_kardex").executeUpdate();
            em.createNativeQuery("TRUNCATE TABLE transacciones").executeUpdate();
            em.createNativeQuery("TRUNCATE TABLE saldos_diarios").executeUpdate();
            em.createNativeQuery("TRUNCATE TABLE saldos").executeUpdate();
            em.createNativeQuery("TRUNCATE TABLE carga_transacciones").executeUpdate();
            
            logger.info("Limpieza de tablas completada exitosamente.");

        } catch (Exception e) {
            logger.error("Error durante la limpieza de tablas.", e);
            // Relanzamos la excepción para que la transacción principal haga rollback.
            throw new RuntimeException("No se pudieron limpiar las tablas de negocio.", e);
        } finally {
            // Es CRUCIAL volver a activar la revisión de llaves foráneas, incluso si hay un error.
            em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1;").executeUpdate();
            logger.debug("Revisión de llaves foráneas reactivada.");
        }
    }
}

