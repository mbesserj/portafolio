package com.app.service;

import com.app.interfaces.AbstractRepository;
import com.app.interfaces.CostingApiInterfaz;
import com.app.interfaces.SaldoApiInterfaz;
import com.app.normalizar.NormalizarDataService;
import com.app.utiles.LibraryInitializer;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contiene la lógica de todos los pasos: limpiar, cargar, normalizar y costear.
 * NOTA: Esta clase podría ser obsoleta y reemplazada por ProcesoCargaInicial.
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

    public void ejecutar() {
        EntityManager em = LibraryInitializer.getEntityManager();
        try {
            em.getTransaction().begin();

            // FASE 1: LIMPIEZA TOTAL
            logger.info("FASE 1/4: Limpiando todas las tablas de datos...");
            new LimpiezaService().limpiarDatosDeNegocio();

            // FASE 2: NORMALIZAR DATOS (DE STAGING A TRANSACCIONENTITY)
            logger.info("FASE 2/4: Normalizando registros y creando Transacciones...");
            // Se le pasa 'true' para indicar que es una Carga Inicial.
            new NormalizarDataService(em).procesar(true);
            em.flush();

            // FASE 3: CREAR SALDOS DE APERTURA DESDE LAS TRANSACCIONES
            logger.info("FASE 3/4: Creando saldos de apertura para el costeo...");
            saldoService.crearSaldosDeAperturaDesdeTransacciones();
            em.flush();

            // FASE 4: EJECUTAR COSTEO FIFO
            logger.info("FASE 4/4: Ejecutando el proceso de costeo FIFO...");
            costingService.ejecutarCosteoCompleto();

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