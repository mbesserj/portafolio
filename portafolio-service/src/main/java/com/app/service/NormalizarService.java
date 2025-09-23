package com.app.service;

import com.app.dto.ResultadoCargaDto;
import com.app.normalizar.NormalizarDataService;
import com.app.utiles.LibraryInitializer;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Orquesta el proceso completo de carga y normalización diaria. Gestiona una
 * única transacción atómica para todo el flujo.
 */
public class NormalizarService {

    private static final Logger logger = LoggerFactory.getLogger(NormalizarService.class);
    private EntityManager em;
    
    public NormalizarService() {
        this.em = em;
    }

    public ResultadoCargaDto ejecutar() {
        em = LibraryInitializer.getEntityManager();
        
        long startTime = System.nanoTime();
        int archivos = 0;
        int trxs = 0;
        int errores = 0;

        try {
            em.getTransaction().begin();
            logger.info("--- INICIANDO PROCESO DE CARGA DIARIA (Transacción Única) ---");

            // FASE 1 (Técnica): Limpiar la caché de persistencia
            em.clear();
            logger.info("Caché de persistencia limpiado para un inicio limpio.");

            // FASE 2: NORMALIZAR DATOS
            logger.info("FASE 1: Normalizando datos y creando relaciones maestras...");
            
            // Se le pasa 'false' para indicar que NO es una Carga Inicial.
            new NormalizarDataService(em, false).procesar();
    
            em.getTransaction().commit();
            logger.info("--- ¡PROCESO DE NORMALIZACION FINALIZADO CON ÉXITO! ---");

            long endTime = System.nanoTime();
            Duration duracion = Duration.ofNanos(endTime - startTime);

            // Al final, en lugar de no devolver nada, creas y devuelves el resumen.
            return new ResultadoCargaDto(trxs, errores, duracion, "Proceso completado.");

        } catch (Exception e) {
            logger.error("Error crítico durante el proceso de carga diaria. Se revertirán TODOS los cambios.", e);
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("El proceso de carga diaria falló.", e);
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }
}
