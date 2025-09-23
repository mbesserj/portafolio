package com.app.normalizar;

import com.app.dao.*;
import com.app.normalizar.EntidadCacheManager;
import com.app.normalizar.NormalizarDatos;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servicio que orquesta el proceso de normalización de datos.
 * Su única responsabilidad es instanciar y ejecutar la clase que contiene la lógica principal.
 */
public class NormalizarDataService {

    private static final Logger logger = LoggerFactory.getLogger(NormalizarDataService.class);
    private final EntityManager em;
    private final boolean esCargaInicial; 

    /**
     * El constructor ahora recibe el EntityManager y el indicador del tipo de carga.
     * @param em El gestor de entidades.
     * @param esCargaInicial 'true' si es una carga de saldos iniciales, 'false' si es una carga diaria.
     */
    public NormalizarDataService(EntityManager em, boolean esCargaInicial) { 
        this.em = em;
        this.esCargaInicial = esCargaInicial;
    }

    /**
     * Ejecuta la lógica de normalización.
     * Asume que ya está dentro de una transacción activa.
     */
    public void procesar() {
        // 1. Se instancian todos los DAOs necesarios.
        CargaTransaccionDao cargaTransaccionDao = new CargaTransaccionDao(em);
        EmpresaDao empresaDao = new EmpresaDao(em);
        CustodioDao custodioDao = new CustodioDao(em);
        TipoMovimientoDao tipoMovimientoDao = new TipoMovimientoDao(em);
        ProductoDao productoDao = new ProductoDao(em);
        InstrumentoDao instrumentoDao = new InstrumentoDao(em, productoDao);
        
        // 2. Se crea el gestor de caché y se le inyectan los DAOs.
        EntidadCacheManager cacheManager = new EntidadCacheManager(
            empresaDao, custodioDao, productoDao, instrumentoDao, tipoMovimientoDao
        );
        
        // 3. Se inyecta el caché y el indicador 'esCargaInicial' en la clase de lógica.
        NormalizarDatos normalizador = new NormalizarDatos(
            em, cargaTransaccionDao, cacheManager, this.esCargaInicial 
        );

        normalizador.procesar();
        logger.info("Lógica de normalización ejecutada.");
    }
}