package com.app.interfaces;

import jakarta.persistence.EntityManager;

/**
 * Repositorio para operaciones especiales de sincronización de datos.
 */
public interface SincronizacionRepInterfaz {
    
    /**
     * Vacía la tabla saldos_kardex y la repuebla con los saldos finales
     * más recientes de la tabla 'saldos'.
     * @param em El EntityManager de la transacción actual.
     */
    void sincronizarSaldosKardexDesdeSaldos(EntityManager em);
}
