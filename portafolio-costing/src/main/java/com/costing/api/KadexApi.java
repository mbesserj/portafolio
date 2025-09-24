package com.costing.api;

import com.app.repositorios.KardexServiceImpl;
import com.app.interfaz.KardexApiInterfaz;

/**
 * Fachada pública y único punto de acceso al módulo de costeo.
 * Esta clase final no contiene lógica de negocio. Su única responsabilidad
 * es construir y configurar correctamente el servicio de costeo con todas
 * sus dependencias internas, ocultando esta complejidad del mundo exterior.

 * Este es el único lugar donde las clases del paquete 'internal' deben ser
 * instanciadas directamente.
 */
public final class KadexApi {

    /**
     * Constructor privado para prevenir que esta clase de utilidad sea instanciada.
     * Todos sus métodos son estáticos.
     */
    private KadexApi() {
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
    public static KardexApiInterfaz createService() {
        
        // --- PASO 1: Obtener la dependencia fundamental: EntityManager ---
        // Usamos la interfaz como tipo de variable para buenas prácticas.
        KardexApiInterfaz kardexService = new KardexServiceImpl();
          
        // --- PASO 2: Devolver el servicio completamente ensamblado ---
        // El cliente que llama a este método no sabe nada sobre KardexServiceImpl,
        // repositorios, o procesos. Solo conoce la interfaz KardexApiInterfaz.
        return kardexService;
    }
}