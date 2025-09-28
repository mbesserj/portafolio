package com.serv.service;

import com.model.dto.ResultadoCargaDto;
import com.model.enums.ListaEnumsCustodios;
import com.model.interfaces.AbstractRepository;
import com.normalizar.process.NormalizarDataService;
import com.etl.service.LectorCartolasService;
import java.io.File;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Orquesta el proceso completo de carga y normalización diaria.
 * Utiliza el patrón AbstractRepository para una gestión transaccional segura.
 */
public class ProcesoCargaDiariaService extends AbstractRepository {

    private static final Logger logger = LoggerFactory.getLogger(ProcesoCargaDiariaService.class);
    
    public ProcesoCargaDiariaService() {
        super();
    }

    public ResultadoCargaDto ejecutar(ListaEnumsCustodios custodio, File file) {
        long startTime = System.nanoTime();

        try {
            logger.info("--- INICIANDO PROCESO DE CARGA DIARIA (Transacción Única y Segura) ---");
            
            // CAMBIO 2: Se usa el helper para manejar la transacción de forma segura.
            // Se elimina todo el bloque try-catch-finally para begin/commit/rollback/close.
            executeInTransaction(em -> {
                // El 'em' que usamos aquí es gestionado por el helper.

                // FASE 1: Limpiar la caché
                em.clear();
                logger.info("Caché de persistencia limpiado.");

                // FASE 2: Cargar datos brutos
                logger.info("FASE 1/2: Cargando datos desde el archivo: {}", file.getName());
                new LectorCartolasService(em).cargar(custodio, file);

                // FASE 3: Normalizar datos
                logger.info("FASE 2/2: Normalizando datos...");
                // Se le pasa 'false' para indicar que NO es una Carga Inicial.
                new NormalizarDataService(em).procesar(false);
            });

            long endTime = System.nanoTime();
            Duration duracion = Duration.ofNanos(endTime - startTime);
            logger.info("--- ¡PROCESO DE CARGA DIARIA FINALIZADO CON ÉXITO! ---");
            
            // NOTA: El conteo de trxs y errores debería implementarse dentro de los servicios.
            return new ResultadoCargaDto(0, 0, duracion, "Proceso completado.");

        } catch (Exception e) {
            // Este catch ahora solo se preocupa de la lógica de negocio (crear el DTO de error),
            // no de la gestión de la transacción.
            logger.error("Error crítico durante el proceso de carga diaria. La transacción ha sido revertida.", e);
            return new ResultadoCargaDto(0, 0, Duration.ZERO, "El proceso falló: " + e.getMessage());
        }
    }
}