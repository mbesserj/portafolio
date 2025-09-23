package com.app.service;

import com.app.entities.InstrumentoEntity;
import com.app.utiles.LibraryInitializer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FusionInstrumentoService {

    private static final Logger logger = LoggerFactory.getLogger(FusionInstrumentoService.class);

    public void fusionarYPrepararRecosteo(Long idInstrumentoAntiguo, Long idInstrumentoNuevo) {
        if (idInstrumentoAntiguo.equals(idInstrumentoNuevo)) {
            throw new IllegalArgumentException("No se puede fusionar un instrumento consigo mismo.");
        }

        EntityManager em = null;
        try {
            em = LibraryInitializer.getEntityManager();
            em.getTransaction().begin();

            logger.info("Iniciando fusión del instrumento ID {} en el instrumento ID {}", idInstrumentoAntiguo, idInstrumentoNuevo);

            InstrumentoEntity instrumentoAntiguo = em.find(InstrumentoEntity.class, idInstrumentoAntiguo);
            InstrumentoEntity instrumentoNuevo = em.find(InstrumentoEntity.class, idInstrumentoNuevo);

            if (instrumentoAntiguo == null || instrumentoNuevo == null) {
                throw new EntityNotFoundException("Uno o ambos instrumentos no fueron encontrados.");
            }

            // 1. LIMPIEZA TOTAL DE DATOS CALCULADOS DE AMBOS INSTRUMENTOS
            logger.info("Limpiando datos calculados para AMBOS instrumentos (ID: {}, ID: {})...", idInstrumentoAntiguo, idInstrumentoNuevo);
            limpiarDatosCalculados("DetalleCosteoEntity", "ingreso.instrumento", instrumentoAntiguo, instrumentoNuevo, em);
            limpiarDatosCalculados("DetalleCosteoEntity", "egreso.instrumento", instrumentoAntiguo, instrumentoNuevo, em);
            limpiarDatosCalculados("KardexEntity", "instrumento", instrumentoAntiguo, instrumentoNuevo, em);
            limpiarDatosCalculados("SaldosDiariosEntity", "instrumento", instrumentoAntiguo, instrumentoNuevo, em);

            // 2. REASIGNAR DATOS FUENTE (NO CALCULADOS)
            reasignarRegistros("TransaccionEntity", "instrumento", instrumentoNuevo, instrumentoAntiguo, em);
            reasignarRegistros("SaldoEntity", "instrumento", instrumentoNuevo, instrumentoAntiguo, em);
            
            // 3. MARCAR TODAS LAS TRANSACCIONES DEL GRUPO UNIFICADO PARA RECOSTEO
            logger.info("Marcando transacciones del instrumento ID {} para recosteo...", idInstrumentoNuevo);
            int transaccionesParaRecostear = em.createQuery("UPDATE TransaccionEntity t SET t.costeado = false WHERE t.instrumento = :instrumento")
                .setParameter("instrumento", instrumentoNuevo)
                .executeUpdate();
            logger.info("{} transacciones marcadas para ser costeadas de nuevo.", transaccionesParaRecostear);

            // 4. ELIMINAR EL INSTRUMENTO ANTIGUO (AHORA SÍ, SIN DEPENDENCIAS)
            logger.info("Eliminando el instrumento antiguo (ID: {})...", idInstrumentoAntiguo);
            em.remove(instrumentoAntiguo);

            em.getTransaction().commit();
            logger.info("Fusión y preparación para recosteo completada con éxito.");

        } catch (Exception e) {
            logger.error("Error durante la fusión. Se revertirán todos los cambios.", e);
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("La fusión falló: " + e.getMessage(), e);
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    private void reasignarRegistros(String entityName, String fieldName, InstrumentoEntity nuevo, InstrumentoEntity antiguo, EntityManager em) {
        logger.info("Reasignando registros de {}...", entityName);
        int actualizados = em.createQuery(String.format("UPDATE %s e SET e.%s = :nuevo WHERE e.%s = :antiguo", entityName, fieldName, fieldName))
            .setParameter("nuevo", nuevo)
            .setParameter("antiguo", antiguo)
            .executeUpdate();
        logger.info("{} registros de {} fueron actualizados.", actualizados, entityName);
    }
    
    private void limpiarDatosCalculados(String entityName, String fieldName, InstrumentoEntity antiguo, InstrumentoEntity nuevo, EntityManager em) {
        int eliminados = em.createQuery(String.format("DELETE FROM %s e WHERE e.%s IN (:antiguo, :nuevo)", entityName, fieldName))
            .setParameter("antiguo", antiguo)
            .setParameter("nuevo", nuevo)
            .executeUpdate();
        logger.info("{} registros de {} fueron eliminados.", eliminados, entityName);
    }
}