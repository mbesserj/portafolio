package com.app.service;

import com.app.normalizar.NormalizarDataService;
import com.app.utiles.LibraryInitializer;
import com.costing.api.CostingApi;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servicio ORQUESTADOR del proceso de Carga Inicial COMPLETA.
 * Contiene la lógica de todos los pasos: limpiar, cargar, normalizar y costear.
 * NOTA: Esta clase podría ser obsoleta y reemplazada por ProcesoCargaInicial.
 */
public class ProcesoCosteoInicial {

    private static final Logger logger = LoggerFactory.getLogger(ProcesoCosteoInicial.class);

    public ProcesoCosteoInicial() {
    }

    public void ejecutar() {
        EntityManager em = LibraryInitializer.getEntityManager();
        try {
            em.getTransaction().begin();

            // FASE 1: LIMPIEZA TOTAL
            logger.info("FASE 1/4: Limpiando todas las tablas de datos...");
            new LimpiezaService(em).limpiarDatosDeNegocio();

            // FASE 2: NORMALIZAR DATOS (DE STAGING A TRANSACCIONENTITY)
            logger.info("FASE 2/4: Normalizando registros y creando Transacciones...");
            // --- CORRECCIÓN APLICADA ---
            // Se le pasa 'true' para indicar que es una Carga Inicial.
            new NormalizarDataService(em, true).procesar();
            em.flush();

            // FASE 3: CREAR SALDOS DE APERTURA DESDE LAS TRANSACCIONES
            logger.info("FASE 3/4: Creando saldos de apertura para el costeo...");
            new SaldoAperturaService(em).crearSaldosDeAperturaDesdeTransacciones();
            em.flush();

            // FASE 4: EJECUTAR COSTEO FIFO
            logger.info("FASE 4/4: Ejecutando el proceso de costeo FIFO...");
            new CostingApi().iniciarCosteoCompleto();

            em.getTransaction().commit();
            logger.info("--- ¡PROCESO DE COSTEO INICIAL FINALIZADO CON ÉXITO! ---");

        } catch (Exception e) {
            logger.error("Error crítico durante el proceso de costeo inicial. Se revertirán los cambios.", e);
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }
}
