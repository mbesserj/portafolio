package com.app.normalizar;

import com.app.dao.*;
import com.app.entities.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NormalizarDatos {

    private static final Logger logger = LoggerFactory.getLogger(NormalizarDatos.class);
    private final EntityManager em;
    private final CargaTransaccionDao cargaTransaccionDao;
    private final EntidadCacheManager cacheManager;
    private final boolean esCargaInicial;

    public NormalizarDatos(EntityManager em, CargaTransaccionDao cargaTransaccionDao, EntidadCacheManager cacheManager, boolean esCargaInicial) {
        this.em = em;
        this.cargaTransaccionDao = cargaTransaccionDao;
        this.cacheManager = cacheManager;
        this.esCargaInicial = esCargaInicial;
    }

    public void procesar() {
        List<CargaTransaccionEntity> registros = cargaTransaccionDao.findUnprocessed();
        logger.info("Se encontraron {} registros para normalizar.", registros.size());

        int exitosos = 0;
        int fallidos = 0;

        for (CargaTransaccionEntity carga : registros) {
            try {
                ProductoEntity producto = cacheManager.getProducto(carga.getProducto());
                EmpresaEntity empresa = cacheManager.getEmpresa(carga.getRazonSocial(), carga.getRut());
                CustodioEntity custodio = cacheManager.getCustodio(carga.getCustodioNombre());
                InstrumentoEntity instrumento = cacheManager.getInstrumento(carga.getInstrumentoNemo(), carga.getInstrumentoNombre(), producto);
                
                TipoMovimientoEntity tipoMovimiento;
                if (esCargaInicial) {
                    tipoMovimiento = cacheManager.getTipoMovimiento("SALDO INICIAL", "Carga de Saldo Inicial");
                } else {
                    tipoMovimiento = cacheManager.getTipoMovimiento(carga.getTipoMovimiento(), "Normalizado desde carga");
                }

                if (empresa == null || custodio == null || instrumento == null || tipoMovimiento == null) {
                    throw new IllegalStateException("Una o más entidades relacionadas no pudieron ser encontradas o creadas.");
                }

                TransaccionEntity transaccion = new TransaccionEntity();
                transaccion.setEmpresa(empresa);
                transaccion.setCustodio(custodio);
                transaccion.setInstrumento(instrumento);                
                transaccion.setTipoMovimiento(tipoMovimiento);
                transaccion.setFecha(carga.getId().getTransactionDate());
                transaccion.setFolio(carga.getFolio());
                transaccion.setCuenta(carga.getCuenta());
                
                // --- ASIGNACIÓN SEGURA DE VALORES NUMÉRICOS ---
                transaccion.setCantidad(Optional.ofNullable(carga.getCantidad()).orElse(BigDecimal.ZERO));
                transaccion.setPrecio(Optional.ofNullable(carga.getPrecio()).orElse(BigDecimal.ZERO));
                transaccion.setTotal(Optional.ofNullable(carga.getMontoTotal()).orElse(BigDecimal.ZERO));
                transaccion.setComisiones(Optional.ofNullable(carga.getComisiones()).orElse(BigDecimal.ZERO));
                transaccion.setGastos(Optional.ofNullable(carga.getGastos()).orElse(BigDecimal.ZERO));
                transaccion.setIva(Optional.ofNullable(carga.getIva()).orElse(BigDecimal.ZERO));
                transaccion.setMonto(Optional.ofNullable(carga.getMonto()).orElse(BigDecimal.ZERO));
                transaccion.setMontoClp(Optional.ofNullable(carga.getMontoClp()).orElse(BigDecimal.ZERO));
                
                transaccion.setMoneda(carga.getMoneda());

                em.persist(transaccion);

                carga.setProcesado(true);
                em.merge(carga);
                exitosos++;

            } catch (Exception e) {
                logger.error("Fallo al normalizar registro de carga con Folio '{}' y Nemo '{}'. Error: {}",
                        carga.getFolio(), carga.getInstrumentoNemo(), e.getMessage(), e);
                fallidos++;
            }
        }
        logger.info("Normalización completada. Registros exitosos: {}, Fallidos: {}.", exitosos, fallidos);
    }
}