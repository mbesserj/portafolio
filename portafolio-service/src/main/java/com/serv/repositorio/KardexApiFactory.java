package com.serv.repositorio;

import com.model.interfaces.KardexApi;
import com.serv.repositorio.KardexServiceImpl;

/**
 * Fábrica pública y único punto de acceso para crear una instancia del servicio de Kardex.
 */
// CAMBIO 1: El nombre de la clase ahora es "Factory"
public final class KardexApiFactory {

    /**
     * Constructor privado para prevenir la instanciación.
     */
    private KardexApiFactory() {
    }

    /**
     * Crea y devuelve una instancia lista para usar del servicio de Kardex.
     * @return Una implementación funcional del servicio, enmascarada por la interfaz KardexApi.
     */
    // CAMBIO 2: El método devuelve la INTERFAZ KardexApi
    public static KardexApi createService() {
        
        // CAMBIO 3: La variable es del tipo de la INTERFAZ
        KardexApi kardexService = new KardexServiceImpl();
        
        return kardexService;
    }
}