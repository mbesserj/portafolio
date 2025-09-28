package com.serv.repositorio;

import com.model.interfaces.SaldoApi; // <-- Importante: Importa la INTERFAZ
import com.serv.repositorio.SaldosServiceImpl;

/**
 * Fábrica pública y único punto de acceso para crear una instancia del servicio de Saldos.
 */
// CAMBIO 1: El nombre de la clase ahora es "Factory"
public final class SaldoApiFactory {

    /**
     * Constructor privado para prevenir la instanciación.
     */
    private SaldoApiFactory() {
    }

    /**
     * Crea y devuelve una instancia lista para usar del servicio de Saldos.
     * @return Una implementación funcional del servicio, enmascarada por la interfaz SaldoApi.
     */
    // CAMBIO 2: El método devuelve la INTERFAZ SaldoApi
    public static SaldoApi createService() {
        
        // CAMBIO 3: La variable es del tipo de la INTERFAZ
        SaldoApi saldoService = new SaldosServiceImpl();
        
        return saldoService;
    }
}