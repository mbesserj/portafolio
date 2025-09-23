package com.app.service;

import com.app.dto.ResultadoCargaDto;
import com.app.enums.ListaEnumsCustodios;
import com.app.normalizar.NormalizarDataService;
import com.app.repository.SincronizacionRepository;
import com.app.utiles.LibraryInitializer;
import com.app.repositorio.SincronizacionRepositoryImpl;
import com.costing.api.CostingApi;
import com.etl.service.LectorCartolasService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.io.File;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servicio ORQUESTADOR del proceso de Carga Inicial COMPLETA.
 * Gestiona transacciones separadas para cada fase para mayor robustez.
 */
public class ProcesoCargaInicial {

    private static final Logger logger = LoggerFactory.getLogger(ProcesoCargaInicial.class);

    public ProcesoCargaInicial() {
        // Constructor
    }

    public ResultadoCargaDto ejecutar(ListaEnumsCustodios custodio, File file) {
        long startTime = System.nanoTime();

        // --- FASE 1: Limpieza ---
        try {
            logger.info("--- INICIANDO PROCESO DE CARGA INICIAL COMPLETA ---");
            logger.info("FASE 1/5: Limpiando todas las tablas de datos...");
            ejecutarEnTransaccion(em -> new LimpiezaService(em).limpiarDatosDeNegocio());
        } catch (Exception e) {
            logger.error("Error crítico en la FASE 1 (Limpieza). El proceso se ha detenido.", e);
            return new ResultadoCargaDto(0, 0, Duration.ZERO, "El proceso falló en la fase de limpieza: " + e.getMessage());
        }

        int registrosLeidos = 0;
        // --- FASE 2: Carga y Normalización ---
        try {
            logger.info("FASE 2/5: Cargando y Normalizando datos desde el archivo: {}", file.getName());
            ResultadoCargaDto resultadoCarga = ejecutarEnTransaccionYRetornar(em -> {
                ResultadoCargaDto resCarga = new LectorCartolasService(em).cargar(custodio, file);
                
                // Se le pasa 'true' para indicar que es una Carga Inicial.
                new NormalizarDataService(em, true).procesar();                
                return resCarga;
            });
            registrosLeidos = resultadoCarga.getRegistrosProcesados();
            
            // Verificación crucial
            long conteoTransacciones = contarTransacciones();
            if (conteoTransacciones == 0 && registrosLeidos > 0) {
                throw new IllegalStateException("La normalización no generó ninguna transacción, aunque se leyeron " + registrosLeidos + " registros. Revisa la lógica de NormalizarDataService.");
            }
            logger.info("Se crearon {} transacciones a partir de {} registros leídos.", conteoTransacciones, registrosLeidos);

        } catch (Exception e) {
            logger.error("Error crítico en la FASE 2 (Carga y Normalización). El proceso se ha detenido.", e);
            return new ResultadoCargaDto(0, 0, Duration.ZERO, "El proceso falló en la fase de normalización: " + e.getMessage());
        }

        // --- FASE 3: Creación de Saldos de Apertura ---
        try {
            logger.info("FASE 3/5: Creando saldos de apertura...");
            ejecutarEnTransaccion(em -> {
                 new SaldoAperturaService(em).crearSaldosDeAperturaDesdeTransacciones();
            });
        } catch (Exception e) {
            logger.error("Error crítico en la FASE 3 (Creación de Saldos). El proceso se ha detenido.", e);
            return new ResultadoCargaDto(registrosLeidos, 0, Duration.ZERO, "El proceso falló al crear saldos de apertura: " + e.getMessage());
        }
        
        // --- FASE 4: Sincronización de Saldos Kardex ---
        try {
            logger.info("FASE 4/5: Sincronizando saldos de Kardex desde la carga inicial...");
            ejecutarEnTransaccion(em -> {
                SincronizacionRepository sincronizador = new SincronizacionRepositoryImpl();
                sincronizador.sincronizarSaldosKardexDesdeSaldos(em);
            });
        
        } catch (Exception e) {
            logger.error("Error crítico en la FASE 4 (Sincronización). El proceso se ha detenido.", e);
            return new ResultadoCargaDto(registrosLeidos, 0, Duration.ZERO, "El proceso falló en la sincronización final: " + e.getMessage());
        }

        // --- FASE 5: Costeo de Datos ---
        try {
            logger.info("FASE 5/5: Costeando datos de la carga inicial...");
            // Esta operación es transaccional por sí misma dentro del servicio.
            new CostingApi().iniciarCosteoCompleto();
        } catch (Exception e) {
            logger.error("Error crítico en la FASE 5 (Costeo). El proceso se ha detenido.", e);
            return new ResultadoCargaDto(registrosLeidos, 0, Duration.ZERO, "El proceso falló en el costeo final: " + e.getMessage());
        }

        long endTime = System.nanoTime();
        Duration duracion = Duration.ofNanos(endTime - startTime);
        logger.info("--- ¡PROCESO DE CARGA INICIAL FINALIZADO CON ÉXITO! ---");
        return new ResultadoCargaDto(registrosLeidos, 0, duracion, "Carga inicial completada. Se procesaron " + registrosLeidos + " registros.");
    }
    
    // --- Métodos de ayuda para la gestión de transacciones ---
    private void ejecutarEnTransaccion(java.util.function.Consumer<EntityManager> action) {
        EntityManager em = null;
        EntityTransaction tx = null;
        try {
            em = LibraryInitializer.getEntityManager();
            tx = em.getTransaction();
            tx.begin();
            action.accept(em);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw e; // Relanzar para que el método principal lo capture
        } finally {
            if (em != null && em.isOpen()) em.close();
        }
    }
    
    private <R> R ejecutarEnTransaccionYRetornar(java.util.function.Function<EntityManager, R> func) {
        EntityManager em = null;
        EntityTransaction tx = null;
        try {
            em = LibraryInitializer.getEntityManager();
            tx = em.getTransaction();
            tx.begin();
            R result = func.apply(em);
            tx.commit();
            return result;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw e;
        } finally {
            if (em != null && em.isOpen()) em.close();
        }
    }
    
    private long contarTransacciones() {
        EntityManager em = LibraryInitializer.getEntityManager();
        try {
            return (long) em.createQuery("SELECT count(t.id) FROM TransaccionEntity t").getSingleResult();
        } finally {
            if (em != null && em.isOpen()) em.close();
        }
    }
}

