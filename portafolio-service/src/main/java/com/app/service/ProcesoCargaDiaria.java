package com.app.service;

import com.app.dto.ResultadoCargaDto;
import com.app.enums.ListaEnumsCustodios;
import com.app.normalizar.NormalizarDataService;
import com.app.utiles.LibraryInitializer;
import com.etl.service.LectorCartolasService;
import jakarta.persistence.EntityManager;
import java.io.File;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Orquesta el proceso completo de carga y normalización diaria.
 * Esta clase vive en 'loader' y cumple el contrato definido en 'core'.
 */
public class ProcesoCargaDiaria { 

    private static final Logger logger = LoggerFactory.getLogger(ProcesoCargaDiaria.class);
    private EntityManager em;
    
    public ProcesoCargaDiaria() {
        // Constructor
    }

    public ResultadoCargaDto ejecutar(ListaEnumsCustodios custodio, File file) {
        em = LibraryInitializer.getEntityManager();
        
        long startTime = System.nanoTime();
        int trxs = 0;
        int errores = 0;

        try {
            em.getTransaction().begin();
            logger.info("--- INICIANDO PROCESO DE CARGA DIARIA (Transacción Única) ---");

            // FASE 1: Limpiar la caché
            em.clear();
            logger.info("Caché de persistencia limpiado.");

            // FASE 2: Cargar datos brutos
            logger.info("FASE 1/2: Cargando datos desde la carpeta: {}", file.getName());
            new LectorCartolasService(em).cargar(custodio, file);

            // FASE 3: Normalizar datos
            logger.info("FASE 2/2: Normalizando datos...");
            // Se le pasa 'false' para indicar que NO es una Carga Inicial.
            new NormalizarDataService(em, false).procesar();

            em.getTransaction().commit();
            logger.info("--- ¡PROCESO DE CARGA DIARIA FINALIZADO CON ÉXITO! ---");

            long endTime = System.nanoTime();
            Duration duracion = Duration.ofNanos(endTime - startTime);
            
            return new ResultadoCargaDto(trxs, errores, duracion, "Proceso completado.");

        } catch (Exception e) {
            logger.error("Error crítico durante el proceso de carga diaria. Se revertirán TODOS los cambios.", e);
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            return new ResultadoCargaDto(0, 0, Duration.ZERO, "El proceso falló: " + e.getMessage());
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }
}
