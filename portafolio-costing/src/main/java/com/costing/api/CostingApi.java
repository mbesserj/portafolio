package com.costing.api;

import com.app.interfaz.CostingApiInterfaz; 
import com.app.repositorios.AjustesProcess;
import com.app.repositorios.CostingServiceImpl;
import com.app.repositorios.KardexServiceImpl;
import com.app.repositorios.ResetCosteoServiceImpl;
import com.app.repositorios.SaldosServiceImpl;
import com.app.repositorios.TipoMovimientoServiceImpl;
import com.app.interfaz.KardexApiInterfaz;
import com.app.interfaz.SaldoApiInterfaz;
import com.app.interfaz.TipoMovimientoInterfaz;

/**
 * Fachada pública y único punto de acceso al módulo de costeo.
 * Esta clase final no contiene lógica de negocio. Su única responsabilidad
 * es construir y configurar correctamente el servicio de costeo con todas
 * sus dependencias internas, ocultando esta complejidad del mundo exterior.

 * Este es el único lugar donde las clases del paquete 'internal' deben ser
 * instanciadas directamente.
 */
public final class CostingApi {

    /**
     * Constructor privado para prevenir que esta clase de utilidad sea instanciada.
     * Todos sus métodos son estáticos.
     */
    private CostingApi() {
    }

    /**
     * Crea, configura y devuelve una instancia lista para usar del servicio de costeo.
     * Este método es el corazón de la fachada. Realiza los siguientes pasos:
     * Instancia cada una de las implementaciones de los repositorios.
     * Instancia los servicios de procesos internos (como AjustesProcess).
     * Construye el servicio principal ( CostingServiceImpl) inyectando todas las dependencias creadas.
     * Devuelve el servicio, pero enmascarado por la interfaz pública CostingApiInterfaz.
     *
     * @return Una implementación funcional y lista del servicio de costeo.
     */
    public static CostingApiInterfaz createService() {
        
        // --- PASO 1: Obtener la dependencia fundamental: EntityManager ---
        // Usamos la interfaz como tipo de variable para buenas prácticas.
        KardexApiInterfaz kardexRepository = new KardexServiceImpl();
        SaldoApiInterfaz saldoRepository = new SaldosServiceImpl();
        TipoMovimientoInterfaz tipoMovimientoRepository = new TipoMovimientoServiceImpl();
        
        // Nota: Para ResetCosteoServiceImpl no tenemos una interfaz en los archivos,
        // pero es buena práctica tenerla. Por ahora, usamos la clase directamente.
        ResetCosteoServiceImpl resetCosteoRepository = new ResetCosteoServiceImpl();

        // --- PASO 2: Instanciar los procesos de negocio internos ---
        AjustesProcess ajustesProcess = new AjustesProcess(saldoRepository, tipoMovimientoRepository, kardexRepository);

        // --- PASO 3: Construir el servicio principal con todas sus dependencias ---
        CostingApiInterfaz costingService = new CostingServiceImpl(
                kardexRepository,
                saldoRepository,
                resetCosteoRepository,
                ajustesProcess
        );

        // --- PASO 4: Devolver el servicio completamente ensamblado ---
        // El cliente que llama a este método no sabe nada sobre CostingServiceImpl,
        // repositorios, o procesos. Solo conoce la interfaz CostingApiInterfaz.
        return costingService;
    }
}