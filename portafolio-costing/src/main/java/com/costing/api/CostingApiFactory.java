package com.costing.api;

import com.costing.repositorios.AjustesProcess;
import com.costing.repositorios.CostingServiceImpl;
import com.serv.repositorio.KardexServiceImpl;
import com.costing.repositorios.ResetCosteoServiceImpl;
import com.serv.repositorio.SaldosServiceImpl;
import com.serv.repositorio.TipoMovimientoServiceImpl;
import com.model.interfaces.CostingApi;
import com.model.interfaces.KardexApi;
import com.model.interfaces.SaldoApi;
import com.model.interfaces.TipoMovimiento;

/**
 * Fábrica pública y único punto de acceso para crear una instancia del servicio de costeo.
 * Su única responsabilidad es construir y configurar correctamente el servicio,
 * ocultando la complejidad de su creación.
 */
public final class CostingApiFactory {

    /**
     * Constructor privado para prevenir que esta clase de utilidad sea instanciada.
     */
    private CostingApiFactory() {
    }

    /**
     * Crea, configura y devuelve una instancia lista para usar del servicio de costeo.
     * @return Una implementación funcional del servicio de costeo, enmascarada por la interfaz CostingApi.
     */
    public static CostingApi createService() {
        
        // --- PASO 1: Instanciar las implementaciones de los repositorios/servicios base ---
        KardexApi kardexRepository = new KardexServiceImpl();
        SaldoApi saldoRepository = new SaldosServiceImpl();
        TipoMovimiento tipoMovimientoRepository = new TipoMovimientoServiceImpl();
        ResetCosteoServiceImpl resetCosteoRepository = new ResetCosteoServiceImpl();

        // --- PASO 2: Instanciar los procesos de negocio internos que dependen de los repositorios ---
        AjustesProcess ajustesProcess = new AjustesProcess(saldoRepository, tipoMovimientoRepository, kardexRepository);

        // --- PASO 3: Construir el servicio principal con TODAS sus dependencias ---
        // La variable ahora es del tipo de la INTERFAZ.
        CostingApi costingService = new CostingServiceImpl(
                kardexRepository,
                saldoRepository,
                resetCosteoRepository,
                ajustesProcess,
                tipoMovimientoRepository 
        );

        // --- PASO 4: Devolver el servicio completamente ensamblado ---
        return costingService;
    }
}