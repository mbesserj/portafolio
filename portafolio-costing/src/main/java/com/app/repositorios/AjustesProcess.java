package com.app.repositorios;

import com.app.dto.AjustePropuestoDto;
import com.app.entities.KardexEntity;
import com.app.entities.SaldoEntity;
import com.app.entities.TipoMovimientoEntity;
import com.app.entities.TransaccionEntity;
import com.app.enums.TipoAjuste;
import com.app.enums.TipoMovimientoEspecial;
import com.app.interfaces.AbstractRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import com.app.interfaces.KardexApiInterfaz;
import com.app.interfaces.SaldoApiInterfaz;
import com.app.interfaces.TipoMovimientoInterfaz;

/**
 * Clase de proceso que encapsula la lógica de negocio para la gestión de ajustes.
 * Utiliza los helpers de AbstractRepository para manejar transacciones y el EntityManager.
 */
public class AjustesProcess extends AbstractRepository {

    private static final Logger logger = LoggerFactory.getLogger(AjustesProcess.class);

    // Los repositorios se mantienen como dependencias inyectadas
    private final SaldoApiInterfaz saldoRepository;
    private final TipoMovimientoInterfaz tipoMovimientoRepository;
    private final KardexApiInterfaz kardexRepository;

    /**
     * Constructor para inyección de dependencias.
     * Recibe el EntityManager y lo pasa a la clase base AbstractRepository.
     */
    public AjustesProcess(SaldoApiInterfaz saldoRepo, TipoMovimientoInterfaz tipoMovRepo, KardexApiInterfaz kardexRepo) {
        super();
        this.saldoRepository = saldoRepo;
        this.tipoMovimientoRepository = tipoMovRepo;
        this.kardexRepository = kardexRepo;
    }

    public AjustePropuestoDto proponerAjusteManual(TransaccionEntity txReferencia, TipoAjuste tipo) {
        // Se envuelve en un método de solo lectura para consistencia
        return executeReadOnly(em -> {
            Optional<KardexEntity> ultimoKardexOpt = this.kardexRepository.obtenerUltimoSaldoAntesDe(txReferencia);

            BigDecimal saldoCantidadAnterior = ultimoKardexOpt.map(KardexEntity::getSaldoCantidad).orElse(BigDecimal.ZERO);
            BigDecimal saldoValorAnterior = ultimoKardexOpt.map(KardexEntity::getSaldoValor).orElse(BigDecimal.ZERO);
            BigDecimal cantidadAjuste;
            BigDecimal precioAjuste;

            if (tipo == TipoAjuste.INGRESO) {
                cantidadAjuste = txReferencia.getCantidad().abs().subtract(saldoCantidadAnterior);
                precioAjuste = (saldoCantidadAnterior.compareTo(BigDecimal.ZERO) > 0)
                        ? saldoValorAnterior.divide(saldoCantidadAnterior, 6, RoundingMode.HALF_UP)
                        : (txReferencia.getPrecio() != null ? txReferencia.getPrecio() : BigDecimal.ZERO);
            } else { // EGRESO
                cantidadAjuste = txReferencia.getCantidad().abs();
                precioAjuste = txReferencia.getPrecio() != null ? txReferencia.getPrecio() : BigDecimal.ZERO;
            }

            String observacion = "Propuesta de ajuste para Tx ID: " + txReferencia.getId();
            AjustePropuestoDto propuesta = new AjustePropuestoDto(txReferencia.getFecha(), "AJUSTE", cantidadAjuste, precioAjuste, observacion);

            Optional<SaldoEntity> saldoAnteriorOpt = this.saldoRepository.buscarSaldoMasCercanoAFecha(
                    txReferencia.getEmpresa().getId(),
                    txReferencia.getCustodio().getId(),
                    txReferencia.getInstrumento().getId(),
                    txReferencia.getCuenta(),
                    txReferencia.getFecha().minusDays(1)
            );

            saldoAnteriorOpt.ifPresent(saldo -> {
                propuesta.setSaldoAnteriorCantidad(saldo.getCantidad());
                propuesta.setSaldoAnteriorFecha(saldo.getFecha());
            });

            return propuesta;
        });
    }

    public void crearAjusteManual(TransaccionEntity txReferencia, TipoAjuste tipo, BigDecimal cantidadFinal, BigDecimal precioFinal) {
        executeInTransaction(em -> {
            BigDecimal montoAjuste = cantidadFinal.multiply(precioFinal);
            String nombreTipoMovimiento = (tipo == TipoAjuste.INGRESO) ? "AJUSTE INGRESO" : "AJUSTE EGRESO";

            TipoMovimientoEntity tipoMovimiento = this.tipoMovimientoRepository.buscarPorNombre(nombreTipoMovimiento)
                    .orElseThrow(() -> new IllegalStateException("El tipo de movimiento '" + nombreTipoMovimiento + "' no existe."));

            TransaccionEntity ajuste = new TransaccionEntity();
            ajuste.setEmpresa(txReferencia.getEmpresa());
            ajuste.setCuenta(txReferencia.getCuenta());
            ajuste.setCustodio(txReferencia.getCustodio());
            ajuste.setInstrumento(txReferencia.getInstrumento());
            ajuste.setFecha(txReferencia.getFecha());
            ajuste.setTipoMovimiento(tipoMovimiento);
            ajuste.setCantidad(cantidadFinal);
            ajuste.setPrecio(precioFinal);
            ajuste.setTotal(montoAjuste);
            ajuste.setGlosa("Ajuste manual creado para la transacción original ID: " + txReferencia.getId());
            ajuste.setCosteado(false);
            ajuste.setParaRevision(false);
            em.persist(ajuste);

            TransaccionEntity original = em.find(TransaccionEntity.class, txReferencia.getId());
            if (original != null && original.isParaRevision()) {
                original.setParaRevision(false);
                em.merge(original);
            }
        });
    }

    public void eliminarAjusteManual(Long idAjuste) {
        executeInTransaction(em -> {
            TransaccionEntity ajusteTx = em.find(TransaccionEntity.class, idAjuste);
            if (ajusteTx == null) {
                logger.warn("No se encontró la transacción de ajuste con ID: {} para eliminar.", idAjuste);
                return; // Salir si no hay nada que eliminar
            }

            String tipoMovimientoNombre = ajusteTx.getTipoMovimiento().getTipoMovimiento();
            TipoMovimientoEspecial tipoEspecial = TipoMovimientoEspecial.fromString(tipoMovimientoNombre);

            if (tipoEspecial == TipoMovimientoEspecial.OTRO) {
                logger.info("La transacción seleccionada ('{}') no es un tipo de ajuste que se pueda eliminar.", tipoMovimientoNombre);
                return;
            }

            if (tipoEspecial == TipoMovimientoEspecial.SALDO_INICIAL || tipoEspecial == TipoMovimientoEspecial.AJUSTE_CUADRATURA) {
                logger.warn("Eliminando ajuste de sistema ({}). Se reseteará el historial de costeo para el grupo: {} | {}",
                        tipoMovimientoNombre, ajusteTx.getInstrumento().getInstrumentoNemo(), ajusteTx.getCuenta());

                em.createQuery("""
                    UPDATE TransaccionEntity t
                    SET t.costeado = false, t.paraRevision = false, t.glosa = NULL
                    WHERE t.empresa = :empresa AND t.custodio = :custodio AND t.instrumento = :instrumento AND t.cuenta = :cuenta
                    """)
                    .setParameter("empresa", ajusteTx.getEmpresa())
                    .setParameter("custodio", ajusteTx.getCustodio())
                    .setParameter("instrumento", ajusteTx.getInstrumento())
                    .setParameter("cuenta", ajusteTx.getCuenta())
                    .executeUpdate();
            }
            logger.info("Eliminando detalles de costeo y kárdex para la transacción de ajuste ID: {}", idAjuste);

            em.createQuery("DELETE FROM DetalleCosteoEntity d WHERE d.ingreso.id = :txId OR d.egreso.id = :txId")
                    .setParameter("txId", idAjuste)
                    .executeUpdate();

            em.createQuery("DELETE FROM KardexEntity k WHERE k.transaccion.id = :txId")
                    .setParameter("txId", idAjuste)
                    .executeUpdate();

            em.remove(ajusteTx);
        });
    }
}