package com.app.repositorios;

import com.app.dto.AjustePropuestoDto;
import com.app.entities.KardexEntity;
import com.app.entities.SaldoEntity;
import com.app.entities.TipoMovimientoEntity;
import com.app.entities.TransaccionEntity;
import com.app.enums.TipoAjuste;
import com.app.enums.TipoMovimientoEspecial;
import com.app.repository.KardexRepository;
import com.app.repository.SaldoRepository;
import com.app.repository.TipoMovimientoRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clase de proceso que encapsula toda la lógica de negocio relacionada con la creación y gestión de ajustes.
 * Es un componente interno del módulo de costeo.
 */
public class AjustesProcess {

    private static final Logger logger = LoggerFactory.getLogger(AjustesProcess.class);

    private EntityManager em;
    private final SaldoRepository saldoRepository;
    private final TipoMovimientoRepository tipoMovimientoRepository;
    private final KardexRepository kardexRepository;

    /**
     * Constructor para inyección de dependencias.
     * Recibe todas las dependencias que necesita para operar.
     */
    public AjustesProcess(EntityManager em, SaldoRepository saldoRepo, TipoMovimientoRepository tipoMovRepo, KardexRepository kardexRepo) {
        this.em = em;
        this.saldoRepository = saldoRepo;
        this.tipoMovimientoRepository = tipoMovRepo;
        this.kardexRepository = kardexRepo;
    }

    public AjustePropuestoDto proponerAjusteManual(TransaccionEntity txReferencia, TipoAjuste tipo) {
        // Usa el repositorio para obtener el último saldo del kárdex
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

        // Usa el repositorio para buscar el saldo más cercano
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
    }

    public void crearAjusteManual(TransaccionEntity txReferencia, TipoAjuste tipo, BigDecimal cantidadFinal, BigDecimal precioFinal) {
        BigDecimal montoAjuste = cantidadFinal.multiply(precioFinal);
        String nombreTipoMovimiento = (tipo == TipoAjuste.INGRESO) ? "AJUSTE INGRESO" : "AJUSTE EGRESO";

        // Usa el repositorio inyectado y desenvuelve el Optional de forma segura
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
    }

    public void eliminarAjusteManual(Long idAjuste)  {
        TransaccionEntity ajusteTx = em.find(TransaccionEntity.class, idAjuste);
        if (ajusteTx == null) {
            try {
                logger.info("No se encontró la transacción con ID: " + idAjuste);
            } catch (Exception ex) {
                System.getLogger(AjustesProcess.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        }

        String tipoMovimiento = ajusteTx.getTipoMovimiento().getTipoMovimiento();
        TipoMovimientoEspecial tipoEspecial = TipoMovimientoEspecial.fromString(tipoMovimiento);

        if (tipoEspecial == TipoMovimientoEspecial.OTRO) {
            try {
                logger.info("La transacción seleccionada ('" + tipoMovimiento + "') no es un tipo de ajuste que se pueda eliminar.");
            } catch (Exception ex) {
                System.getLogger(AjustesProcess.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        }

        if (tipoEspecial == TipoMovimientoEspecial.SALDO_INICIAL || tipoEspecial == TipoMovimientoEspecial.AJUSTE_CUADRATURA) {
            logger.warn("Eliminando ajuste de sistema ({}). Se reseteará el historial de costeo para el grupo: {} | {}",
                    tipoMovimiento, ajusteTx.getInstrumento().getInstrumentoNemo(), ajusteTx.getCuenta());

            em.createQuery("""
                UPDATE TransaccionEntity t
                SET t.costeado = false, t.paraRevision = false, t.glosa = NULL
                WHERE t.empresa = :empresa
                  AND t.custodio = :custodio
                  AND t.instrumento = :instrumento
                  AND t.cuenta = :cuenta
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
    }

    /**
     * Helper privado para obtener transacciones ordenadas.
     * NOTA: Esta lógica de consulta es una candidata ideal para ser movida a un futuro TransaccionRepository.
     */
    private List<TransaccionEntity> obtenerTransaccionesOrdenadas(EntityManager em, Long empresaId, Long custodioId, Long instrumentoId, String cuenta, String orden) {
        String jpql = String.format("""
            SELECT t FROM TransaccionEntity t
            JOIN FETCH t.tipoMovimiento tm
            JOIN FETCH tm.movimientoContable
            WHERE t.empresa.id = :empresaId
              AND t.custodio.id = :custodioId
              AND t.instrumento.id = :instrumentoId
              AND t.cuenta = :cuenta
            ORDER BY t.fecha %s, t.id %s
            """, orden, orden);

        return em.createQuery(jpql, TransaccionEntity.class)
                .setParameter("empresaId", empresaId)
                .setParameter("custodioId", custodioId)
                .setParameter("instrumentoId", instrumentoId)
                .setParameter("cuenta", cuenta)
                .getResultList();
    }
}