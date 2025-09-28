package com.serv.service;

import com.app.normalizar.NormalizarDataService;
import com.model.dto.ResultadoCargaDto;
import com.model.enums.ListaEnumsCustodios;
import com.model.exception.CostingException;
import com.model.interfaces.AbstractRepository;
import com.etl.service.LectorCartolasService;
import java.io.File;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.model.interfaces.CostingApi;
import com.model.interfaces.KardexApi;
import com.model.interfaces.SaldoApi;

/**
 * Gestiona transacciones separadas para cada fase para mayor robustez.
 */
public class ProcesoCargaInicialService extends AbstractRepository {

    private static final Logger logger = LoggerFactory.getLogger(ProcesoCargaInicialService.class);

    private final CostingApi costingService;
    private final SaldoApi saldoService;
    private final KardexApi kardexService;

    public ProcesoCargaInicialService(CostingApi costingService, SaldoApi saldoService, KardexApi kardexService) {
        super();
        this.costingService = costingService;
        this.saldoService = saldoService;
        this.kardexService = kardexService;
    }

    public ResultadoCargaDto ejecutar(ListaEnumsCustodios custodio, File file) {
        long startTime = System.nanoTime();

        // --- FASE 1: Limpieza ---
        try {
            logger.info("--- INICIANDO PROCESO DE CARGA INICIAL COMPLETA ---");
            logger.info("FASE 1/5: Limpiando todas las tablas de datos...");
            executeInTransaction(em -> {
                new LimpiezaService().limpiarDatosDeNegocio();
            });
        } catch (Exception e) {
            logger.error("Error crítico en la FASE 1 (Limpieza). El proceso se ha detenido.", e);
            return new ResultadoCargaDto(0, 0, Duration.ZERO, "El proceso falló en la fase de limpieza: " + e.getMessage());
        }

        int registrosLeidos = 0;
        // --- FASE 2: Carga y Normalización ---
        try {
            logger.info("FASE 2/5: Cargando y Normalizando datos desde el archivo: {}", file.getName());
            
            /**
             * pasa true para una carga inicial.
             */
            ResultadoCargaDto resultadoCarga = executeInTransaction(em -> {
                ResultadoCargaDto resCarga = (ResultadoCargaDto) new LectorCartolasService(em).cargar(custodio, file);
                new NormalizarDataService(em).procesar(true);
                return resCarga;
            });
            registrosLeidos = resultadoCarga.getRegistrosProcesados();

            long conteoTransacciones = contarTransacciones();
            if (conteoTransacciones == 0 && registrosLeidos > 0) {
                throw new IllegalStateException("La normalización no generó ninguna transacción, aunque se leyeron " + registrosLeidos + " registros. Revisa la lógica de NormalizarDataService.");
            }
            logger.info("Se crearon {} transacciones a partir de {} registros leídos.", conteoTransacciones, registrosLeidos);

        } catch (IllegalStateException e) {
            logger.error("Error crítico en la FASE 2 (Carga y Normalización). El proceso se ha detenido.", e);
            return new ResultadoCargaDto(0, 0, Duration.ZERO, "El proceso falló en la fase de normalización: " + e.getMessage());
        }

        // --- FASE 3: Creación de Saldos de Apertura ---
        try {
            logger.info("FASE 3/5: Creando saldos de apertura...");
            executeInTransaction(em -> {
                saldoService.crearSaldosDeAperturaDesdeTransacciones();
            });
        } catch (Exception e) {
            logger.error("Error crítico en la FASE 3 (Creación de Saldos). El proceso se ha detenido.", e);
            return new ResultadoCargaDto(registrosLeidos, 0, Duration.ZERO, "El proceso falló al crear saldos de apertura: " + e.getMessage());
        }

        // --- FASE 4: Sincronización de Saldos Kardex ---
        try {
            logger.info("FASE 4/5: Sincronizando saldos de Kardex desde la carga inicial...");
            executeInTransaction(em -> {
                kardexService.sincronizarSaldosKardexDesdeSaldos();
            });

        } catch (Exception e) {
            logger.error("Error crítico en la FASE 4 (Sincronización). El proceso se ha detenido.", e);
            return new ResultadoCargaDto(registrosLeidos, 0, Duration.ZERO, "El proceso falló en la sincronización final: " + e.getMessage());
        }

        // --- FASE 5: Costeo de Datos ---
        try {
            logger.info("FASE 5/5: Costeando datos de la carga inicial...");
            costingService.ejecutarCosteoCompleto();
        } catch (CostingException e) {
            logger.error("Error crítico en la FASE 5 (Costeo). El proceso se ha detenido.", e);
            return new ResultadoCargaDto(registrosLeidos, 0, Duration.ZERO, "El proceso falló en el costeo final: " + e.getMessage());
        }

        long endTime = System.nanoTime();
        Duration duracion = Duration.ofNanos(endTime - startTime);
        logger.info("--- ¡PROCESO DE CARGA INICIAL FINALIZADO CON ÉXITO! ---");
        return new ResultadoCargaDto(registrosLeidos, 0, duracion, "Carga inicial completada. Se procesaron " + registrosLeidos + " registros.");
    }

    private long contarTransacciones() {
        return executeReadOnly(em
                -> em.createQuery("SELECT count(t.id) FROM TransaccionEntity t", Long.class)
                        .getSingleResult()
        );
    }
}