package com.app.normalizar;

import com.app.dao.*;
import com.app.entities.*;

import java.util.HashMap;
import java.util.Map;

public class EntidadCacheManager {

    private final EmpresaDao empresaDao;
    private final CustodioDao custodioDao;
    private final ProductoDao productoDao;
    private final InstrumentoDao instrumentoDao;
    private final TipoMovimientoDao tipoMovimientoDao;

    private final Map<String, EmpresaEntity> empresaCache = new HashMap<>();
    private final Map<String, CustodioEntity> custodioCache = new HashMap<>();
    private final Map<String, ProductoEntity> productoCache = new HashMap<>();
    private final Map<String, InstrumentoEntity> instrumentoCache = new HashMap<>();
    private final Map<String, TipoMovimientoEntity> tipoMovimientoCache = new HashMap<>();

    public EntidadCacheManager(EmpresaDao empresaDao,
            CustodioDao custodioDao,
            ProductoDao productoDao,
            InstrumentoDao instrumentoDao,
            TipoMovimientoDao tipoMovimientoDao) {
        this.empresaDao = empresaDao;
        this.custodioDao = custodioDao;
        this.productoDao = productoDao;
        this.instrumentoDao = instrumentoDao;
        this.tipoMovimientoDao = tipoMovimientoDao;
    }

    public EmpresaEntity getEmpresa(String razonSocial, String rut) {
        // La clave del caché es el RUT normalizado, que es el identificador único.
        String rutNormalizado = (rut == null) ? "" : rut.replace(".", "").replace("-", "").toUpperCase();
        if (rutNormalizado.isEmpty()) {
            return null;
        }

        return empresaCache.computeIfAbsent(rutNormalizado, k -> empresaDao.findOrCreateByRazonSocial(razonSocial, rut));
    }

    public CustodioEntity getCustodio(String nombre) {
        String nombreNormalizado = nombre.trim().replace("Peshing", "Pershing");
        return custodioCache.computeIfAbsent(nombreNormalizado, k -> custodioDao.findOrCreateByNombre(nombreNormalizado));
    }

    public ProductoEntity getProducto(String cuenta) {
        return productoCache.computeIfAbsent(cuenta, k -> productoDao.findOrCreateByProducto(cuenta));
    }

    public InstrumentoEntity getInstrumento(String nemo, String nombre, ProductoEntity producto) {
        String key = nemo + "|" + nombre + "|" + (producto != null ? producto.getId() : "null");
        return instrumentoCache.computeIfAbsent(key, k -> instrumentoDao.findOrCreateByInstrumento(nemo, nombre, producto));
    }

    public TipoMovimientoEntity getTipoMovimiento(String tipoMovimiento, String descripcion) {
        return tipoMovimientoCache.computeIfAbsent(tipoMovimiento, k -> tipoMovimientoDao.findOrCreateByTipoMovimiento(tipoMovimiento, descripcion));
    }
}
