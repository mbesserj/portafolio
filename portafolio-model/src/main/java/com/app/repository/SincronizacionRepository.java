package com.app.repository;

import jakarta.persistence.EntityManager;

/**
 * Repositorio para operaciones especiales de sincronización de datos.
 */
public interface SincronizacionRepository {
    
    /**
     * Vacía la tabla saldos_kardex y la repuebla con los saldos finales
     * más recientes de la tabla 'saldos'.
     * @param em El EntityManager de la transacción actual.
     */
    void sincronizarSaldosKardexDesdeSaldos(EntityManager em);
}
