package com.app.service;

import com.app.interfaces.AbstractRepository;
import com.app.interfaces.CostingApiInterfaz;
import com.app.interfaces.SaldoApiInterfaz;
import com.app.normalizar.NormalizarDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servicio refactorizado que usa AbstractRepository para manejo seguro de transacciones.
 * Elimina completamente el manejo manual de EntityManager y transacciones.
 */
public class ProcesoCosteoInicialService extends AbstractRepository {

    private static final Logger logger = LoggerFactory.getLogger(ProcesoCosteoInicialService.class);

    private final CostingApiInterfaz costingService;
    private final SaldoApiInterfaz saldoService;
    
    public ProcesoCosteoInicialService(CostingApiInterfaz costingService, SaldoApiInterfaz saldoService) {
        super();
        this.costingService = costingService;
        this.saldoService = saldoService;
    }

    /**
     * Ejecuta el proceso completo de costeo inicial usando transacciones seguras.
     * Cada fase se ejecuta en su propia transacción para mejor control y recuperación.
     * 
     * @return Resultado del procesamiento
     */
    public ProcessResult ejecutar() {
        logger.info("--- INICIANDO PROCESO DE COSTEO INICIAL ---");
        
        try {
            // FASE 1: LIMPIEZA TOTAL - Transacción independiente
            ProcessResult limpiezaResult = ejecutarLimpieza();
            if (!limpiezaResult.isSuccess()) {
                return limpiezaResult;
            }

            // FASE 2: NORMALIZACIÓN - Transacción independiente
            ProcessResult normalizacionResult = ejecutarNormalizacion();
            if (!normalizacionResult.isSuccess()) {
                return normalizacionResult;
            }

            // FASE 3: CREACIÓN DE SALDOS - Transacción independiente
            ProcessResult saldosResult = ejecutarCreacionSaldos();
            if (!saldosResult.isSuccess()) {
                return saldosResult;
            }

            // FASE 4: COSTEO FIFO - Transacción independiente
            ProcessResult costeoResult = ejecutarCosteo();
            if (!costeoResult.isSuccess()) {
                return costeoResult;
            }

            logger.info("--- PROCESO DE COSTEO INICIAL FINALIZADO CON ÉXITO ---");
            return ProcessResult.success("Proceso de costeo inicial completado exitosamente");

        } catch (Exception e) {
            logger.error("Error crítico en el proceso de costeo inicial", e);
            return ProcessResult.failure("Error crítico en el proceso: " + e.getMessage());
        }
    }

    /**
     * FASE 1: Limpieza de datos usando transacción segura.
     */
    private ProcessResult ejecutarLimpieza() {
        logger.info("FASE 1/4: Limpiando todas las tablas de datos...");
        
        return executeInTransactionWithResult(em -> {
            try {
                new LimpiezaService().limpiarDatosDeNegocio();
                logger.info("Limpieza completada exitosamente");
                return ProcessResult.success("Limpieza completada");
                
            } catch (Exception e) {
                logger.error("Error en la fase de limpieza", e);
                throw new ProcessException("Error en limpieza: " + e.getMessage(), e);
            }
        });
    }

    /**
     * FASE 2: Normalización de datos usando transacción segura.
     */
    private ProcessResult ejecutarNormalizacion() {
        logger.info("FASE 2/4: Normalizando registros y creando Transacciones...");
        
        return executeInTransactionWithResult(em -> {
            try {
                // Normalizar datos con flag de carga inicial
                new NormalizarDataService(em).procesar(true);
                
                // Flush para asegurar que los datos se escriben
                em.flush();
                
                // Verificar que se crearon transacciones
                long conteoTransacciones = contarTransacciones(em);
                if (conteoTransacciones == 0) {
                    throw new ProcessException("La normalización no generó ninguna transacción");
                }
                
                logger.info("Normalización completada. Se crearon {} transacciones", conteoTransacciones);
                return ProcessResult.success("Normalización completada con " + conteoTransacciones + " transacciones");
                
            } catch (Exception e) {
                logger.error("Error en la fase de normalización", e);
                throw new ProcessException("Error en normalización: " + e.getMessage(), e);
            }
        });
    }

    /**
     * FASE 3: Creación de saldos de apertura usando transacción segura.
     */
    private ProcessResult ejecutarCreacionSaldos() {
        logger.info("FASE 3/4: Creando saldos de apertura para el costeo...");
        
        return executeInTransactionWithResult(em -> {
            try {
                saldoService.crearSaldosDeAperturaDesdeTransacciones();
                
                // Flush para asegurar persistencia
                em.flush();
                
                logger.info("Saldos de apertura creados exitosamente");
                return ProcessResult.success("Saldos de apertura creados");
                
            } catch (Exception e) {
                logger.error("Error en la fase de creación de saldos", e);
                throw new ProcessException("Error en creación de saldos: " + e.getMessage(), e);
            }
        });
    }

    /**
     * FASE 4: Ejecución del costeo FIFO usando transacción segura.
     */
    private ProcessResult ejecutarCosteo() {
        logger.info("FASE 4/4: Ejecutando el proceso de costeo FIFO...");
        
        try {
            // El costeo puede manejar sus propias transacciones internamente
            costingService.ejecutarCosteoCompleto();
            
            logger.info("Costeo FIFO ejecutado exitosamente");
            return ProcessResult.success("Costeo FIFO completado");
            
        } catch (Exception e) {
            logger.error("Error en la fase de costeo", e);
            return ProcessResult.failure("Error en costeo: " + e.getMessage());
        }
    }

    /**
     * Cuenta el número de transacciones en la base de datos.
     */
    private long contarTransacciones(jakarta.persistence.EntityManager em) {
        return em.createQuery("SELECT count(t.id) FROM TransaccionEntity t", Long.class)
                 .getSingleResult();
    }

    /**
     * Clase de resultado para operaciones de procesamiento.
     */
    public static class ProcessResult {
        private final boolean success;
        private final String message;
        private final Exception exception;

        private ProcessResult(boolean success, String message, Exception exception) {
            this.success = success;
            this.message = message;
            this.exception = exception;
        }

        public static ProcessResult success(String message) {
            return new ProcessResult(true, message, null);
        }

        public static ProcessResult failure(String message) {
            return new ProcessResult(false, message, null);
        }

        public static ProcessResult failure(String message, Exception exception) {
            return new ProcessResult(false, message, exception);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Exception getException() { return exception; }
    }

    /**
     * Excepción específica para errores de procesamiento.
     */
    public static class ProcessException extends RuntimeException {
        public ProcessException(String message) {
            super(message);
        }

        public ProcessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}