package com.service.service;

import com.app.entities.InstrumentoEntity;
import com.app.interfaces.AbstractRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FusionInstrumentoService extends AbstractRepository {

    private static final Logger logger = LoggerFactory.getLogger(FusionInstrumentoService.class);

    public FusionInstrumentoService() {
        super();
    }
    
    /**
     * Fusiona dos instrumentos y prepara todas las transacciones para recosteo.
     * 
     * @param idInstrumentoAntiguo ID del instrumento que será eliminado
     * @param idInstrumentoNuevo ID del instrumento que permanecerá
     * @throws IllegalArgumentException si los parámetros son inválidos
     * @throws EntityNotFoundException si alguno de los instrumentos no existe
     */
    public void fusionarYPrepararRecosteo(Long idInstrumentoAntiguo, Long idInstrumentoNuevo) {
        // Validaciones de entrada
        if (idInstrumentoAntiguo == null) {
            throw new IllegalArgumentException("El ID del instrumento antiguo no puede ser null");
        }
        if (idInstrumentoNuevo == null) {
            throw new IllegalArgumentException("El ID del instrumento nuevo no puede ser null");
        }
        if (idInstrumentoAntiguo.equals(idInstrumentoNuevo)) {
            throw new IllegalArgumentException("No se puede fusionar un instrumento consigo mismo");
        }

        executeInTransaction(em -> {
            logger.info("Iniciando fusión del instrumento ID {} en el instrumento ID {}", 
                       idInstrumentoAntiguo, idInstrumentoNuevo);

            // Verificar existencia de ambos instrumentos
            InstrumentoEntity instrumentoAntiguo = em.find(InstrumentoEntity.class, idInstrumentoAntiguo);
            InstrumentoEntity instrumentoNuevo = em.find(InstrumentoEntity.class, idInstrumentoNuevo);

            if (instrumentoAntiguo == null) {
                throw new EntityNotFoundException("Instrumento antiguo no encontrado con ID: " + idInstrumentoAntiguo);
            }
            if (instrumentoNuevo == null) {
                throw new EntityNotFoundException("Instrumento nuevo no encontrado con ID: " + idInstrumentoNuevo);
            }

            logger.info("Fusionando instrumento '{}' ({}) en instrumento '{}' ({})",
                       instrumentoAntiguo.getInstrumentoNemo(), idInstrumentoAntiguo,
                       instrumentoNuevo.getInstrumentoNemo(), idInstrumentoNuevo);

            // 1. LIMPIEZA TOTAL DE DATOS CALCULADOS DE AMBOS INSTRUMENTOS
            logger.info("Limpiando datos calculados para AMBOS instrumentos...");
            limpiarDatosCalculados("DetalleCosteoEntity", "ingreso.instrumento", instrumentoAntiguo, instrumentoNuevo, em);
            limpiarDatosCalculados("DetalleCosteoEntity", "egreso.instrumento", instrumentoAntiguo, instrumentoNuevo, em);
            limpiarDatosCalculados("KardexEntity", "instrumento", instrumentoAntiguo, instrumentoNuevo, em);
            limpiarDatosCalculados("SaldosDiariosEntity", "instrumento", instrumentoAntiguo, instrumentoNuevo, em);

            // 2. REASIGNAR DATOS FUENTE (NO CALCULADOS)
            reasignarRegistros("TransaccionEntity", "instrumento", instrumentoNuevo, instrumentoAntiguo, em);
            reasignarRegistros("SaldoEntity", "instrumento", instrumentoNuevo, instrumentoAntiguo, em);
            
            // 3. MARCAR TODAS LAS TRANSACCIONES DEL GRUPO UNIFICADO PARA RECOSTEO
            logger.info("Marcando transacciones del instrumento ID {} para recosteo...", idInstrumentoNuevo);
            int transaccionesParaRecostear = em.createQuery(
                "UPDATE TransaccionEntity t SET t.costeado = false WHERE t.instrumento = :instrumento")
                .setParameter("instrumento", instrumentoNuevo)
                .executeUpdate();
            logger.info("{} transacciones marcadas para ser costeadas de nuevo", transaccionesParaRecostear);

            // 4. ELIMINAR EL INSTRUMENTO ANTIGUO
            logger.info("Eliminando el instrumento antiguo (ID: {})...", idInstrumentoAntiguo);
            em.remove(instrumentoAntiguo);

            logger.info("Fusión y preparación para recosteo completada con éxito");
        });
    }

    private void reasignarRegistros(String entityName, String fieldName, InstrumentoEntity nuevo, 
                                  InstrumentoEntity antiguo, jakarta.persistence.EntityManager em) {
        logger.debug("Reasignando registros de {}...", entityName);
        int actualizados = em.createQuery(String.format(
            "UPDATE %s e SET e.%s = :nuevo WHERE e.%s = :antiguo", entityName, fieldName, fieldName))
            .setParameter("nuevo", nuevo)
            .setParameter("antiguo", antiguo)
            .executeUpdate();
        logger.info("{} registros de {} fueron actualizados", actualizados, entityName);
    }
    
    private void limpiarDatosCalculados(String entityName, String fieldName, InstrumentoEntity antiguo, 
                                      InstrumentoEntity nuevo, jakarta.persistence.EntityManager em) {
        logger.debug("Limpiando datos calculados de {}...", entityName);
        int eliminados = em.createQuery(String.format(
            "DELETE FROM %s e WHERE e.%s IN (:antiguo, :nuevo)", entityName, fieldName))
            .setParameter("antiguo", antiguo)
            .setParameter("nuevo", nuevo)
            .executeUpdate();
        logger.info("{} registros de {} fueron eliminados", eliminados, entityName);
    }
}