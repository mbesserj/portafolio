package com.app.service;

import com.app.dto.ResultadoCargaDto;
import com.app.interfaces.AbstractRepository;
import com.app.normalizar.NormalizarDataService;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Orquesta el proceso completo de normalización diaria.
 */
public class NormalizarService extends AbstractRepository {

    private static final Logger logger = LoggerFactory.getLogger(NormalizarService.class);
    
    /**
     * Constructor.
     */
    public NormalizarService() {
        super();
    }
    
    /**
     * Ejecuta el proceso de normalización dentro de una transacción única.
     * 
     * @return Resultado del proceso con información de duración y estado
     * @throws RuntimeException si falla el proceso de normalización
     */
    public ResultadoCargaDto ejecutar() {
        long startTime = System.nanoTime();

        return executeInTransaction(em -> {
            logger.info("--- INICIANDO PROCESO DE NORMALIZACIÓN (Transacción Única) ---");

            try {
                // FASE 1: Limpiar la caché de persistencia
                em.clear();
                logger.info("Caché de persistencia limpiado para un inicio limpio");

                // FASE 2: NORMALIZAR DATOS
                logger.info("Normalizando datos y creando relaciones maestras...");
                new NormalizarDataService(em).procesar(false);

                logger.info("--- ¡PROCESO DE NORMALIZACIÓN FINALIZADO CON ÉXITO! ---");

                long endTime = System.nanoTime();
                Duration duracion = Duration.ofNanos(endTime - startTime);

                return new ResultadoCargaDto(0, 0, duracion, "Proceso completado exitosamente");

            } catch (Exception e) {
                logger.error("Error durante el proceso de normalización", e);
                throw new RuntimeException("Falló el proceso de normalización: " + e.getMessage(), e);
            }
        });
    }
}