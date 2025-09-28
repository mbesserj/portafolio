package com.serv.repositorio;

import com.model.interfaces.AbstractRepository;
import com.model.dto.ResumenSaldoDto;
import com.model.entities.CustodioEntity;
import com.model.entities.EmpresaEntity;
import com.model.entities.InstrumentoEntity;
import com.model.entities.SaldoEntity;
import com.model.entities.SaldoKardexEntity;
import com.model.entities.SaldosDiariosEntity;
import com.model.entities.TipoMovimientoEntity;
import com.model.entities.TransaccionEntity;
import jakarta.persistence.NoResultException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import com.model.interfaces.SaldoApi;
import java.math.BigDecimal;

/**
 * Implementación del repositorio para acceder a los datos de Saldos Diarios.
 */
public class SaldosServiceImpl extends AbstractRepository implements SaldoApi {

    public SaldosServiceImpl() {
        super();
    }

    @Override
    public Optional<SaldosDiariosEntity> findLastBeforeDate(EmpresaEntity empresa, CustodioEntity custodio, InstrumentoEntity instrumento, String cuenta, LocalDate fecha) {
        return execute(em -> em.createQuery("SELECT s FROM SaldosDiariosEntity s WHERE s.empresa = :empresa AND s.custodio = :custodio AND s.instrumento = :instrumento AND s.cuenta = :cuenta AND s.fecha < :fecha ORDER BY s.fecha DESC", SaldosDiariosEntity.class)
                .setParameter("empresa", empresa).setParameter("custodio", custodio).setParameter("instrumento", instrumento)
                .setParameter("cuenta", cuenta).setParameter("fecha", fecha).setMaxResults(1).getResultList().stream().findFirst());
    }

    @Override
    public List<SaldosDiariosEntity> findAllByGroupAndDateRange(EmpresaEntity empresa, CustodioEntity custodio, InstrumentoEntity instrumento, String cuenta, LocalDate fechaInicio, LocalDate fechaFin) {
        return execute(em -> em.createQuery("SELECT s FROM SaldosDiariosEntity s WHERE s.empresa = :empresa AND s.custodio = :custodio AND s.instrumento = :instrumento AND s.cuenta = :cuenta AND s.fecha BETWEEN :fechaInicio AND :fechaFin", SaldosDiariosEntity.class)
                .setParameter("empresa", empresa).setParameter("custodio", custodio).setParameter("instrumento", instrumento)
                .setParameter("cuenta", cuenta).setParameter("fechaInicio", fechaInicio).setParameter("fechaFin", fechaFin).getResultList());
    }

    @Override
    public Optional<SaldoEntity> buscarSaldoMasCercanoAFecha(Long empresaId, Long custodioId, Long instrumentoId, String cuenta, LocalDate fechaObjetivo) {
        return execute(em -> {
            try {
                return Optional.of(em.createQuery("SELECT s FROM SaldoEntity s WHERE s.empresa.id = :empresaId AND s.custodio.id = :custodioId AND s.instrumento.id = :instrumentoId AND s.cuenta = :cuenta AND s.fecha <= :fechaObjetivo ORDER BY s.fecha DESC", SaldoEntity.class)
                        .setParameter("empresaId", empresaId).setParameter("custodioId", custodioId).setParameter("instrumentoId", instrumentoId)
                        .setParameter("cuenta", cuenta).setParameter("fechaObjetivo", fechaObjetivo).setMaxResults(1).getSingleResult());
            } catch (NoResultException e) {
                return Optional.empty();
            }
        });
    }

    @Override
    public List<ResumenSaldoDto> obtenerResumenSaldosAgregados(Long empresaId, Long custodioId) {
        return executeReadOnly(em
                -> em.createQuery("""
                SELECT NEW com.app.dto.ResumenSaldoDto(
                    s.instrumento.id,
                    s.instrumento.instrumentoNemo,
                    s.instrumento.instrumentoNombre,
                    s.empresa.id,
                    s.custodio.id,
                    s.saldoCantidad,
                    s.saldoValor,
                    (CASE WHEN s.saldoCantidad <> 0 THEN s.saldoValor / s.saldoCantidad ELSE 0 END),
                    java.math.BigDecimal.ZERO,
                    java.math.BigDecimal.ZERO,
                    java.math.BigDecimal.ZERO,
                    java.math.BigDecimal.ZERO
                )
                FROM SaldosDiariosEntity s
                WHERE s.empresa.id = :empresaId
                  AND s.custodio.id = :custodioId
                  AND s.fecha = (
                      SELECT MAX(s2.fecha)
                      FROM SaldosDiariosEntity s2
                      WHERE s2.instrumento.id = s.instrumento.id
                        AND s2.cuenta = s.cuenta
                        AND s2.empresa.id = :empresaId
                        AND s2.custodio.id = :custodioId
                  )
                AND s.saldoCantidad > 0
                ORDER BY s.instrumento.instrumentoNemo, s.cuenta
                """, ResumenSaldoDto.class)
                        .setParameter("empresaId", empresaId)
                        .setParameter("custodioId", custodioId)
                        .getResultList()
        );
    }

    @Override
    public Optional<SaldoEntity> obtenerUltimoSaldo(Long empresaId, Long custodioId, String cuenta, Long instrumentoId) {
        return executeReadOnly(em
                -> em.createQuery("""
                SELECT s FROM SaldoEntity s
                WHERE s.empresa.id = :empresaId
                  AND s.custodio.id = :custodioId
                  AND s.instrumento.id = :instrumentoId
                  AND s.cuenta = :cuenta
                ORDER BY s.fecha DESC
                """, SaldoEntity.class)
                        .setParameter("empresaId", empresaId)
                        .setParameter("custodioId", custodioId)
                        .setParameter("instrumentoId", instrumentoId)
                        .setParameter("cuenta", cuenta)
                        .setMaxResults(1)
                        .getResultList().stream().findFirst()
        );
    }

    @Override
    public Optional<SaldoKardexEntity> findByGrupo(Long empresaId, Long custodioId, Long instrumentoId, String cuenta) {
        return execute(em -> {
            try {
                return Optional.of(em.createQuery("SELECT sk FROM SaldoKardexEntity sk WHERE sk.empresa.id = :empresaId AND sk.custodio.id = :custodioId AND sk.instrumento.id = :instrumentoId AND sk.cuenta = :cuenta", SaldoKardexEntity.class)
                        .setParameter("empresaId", empresaId).setParameter("custodioId", custodioId)
                        .setParameter("instrumentoId", instrumentoId).setParameter("cuenta", cuenta).getSingleResult());
            } catch (NoResultException e) {
                return Optional.empty();
            }
        });
    }

    /**
     * Crea el saldo de apertura la primera vez que se cargan datos.
     */
    @Override
    public void crearSaldosDeAperturaDesdeTransacciones() {

        executeInTransaction(em -> {
            TipoMovimientoEntity tipoMovimientoSaldoInicial;
            try {
                tipoMovimientoSaldoInicial = em.createQuery(
                        "SELECT tm FROM TipoMovimientoEntity tm WHERE tm.tipoMovimiento = 'SALDO INICIAL'",
                        TipoMovimientoEntity.class
                ).getSingleResult();
            } catch (NoResultException e) {
                throw new IllegalStateException("El tipo de movimiento 'SALDO INICIAL' debe existir para la carga inicial.");
            }

            List<TransaccionEntity> transaccionesDeApertura = em.createQuery(
                    "SELECT t FROM TransaccionEntity t WHERE t.tipoMovimiento = :tipoMov",
                    TransaccionEntity.class
            ).setParameter("tipoMov", tipoMovimientoSaldoInicial).getResultList();

            if (transaccionesDeApertura.isEmpty()) {
                return; // Salimos de la transacción y del método.
            }

            for (TransaccionEntity tx : transaccionesDeApertura) {
                SaldoEntity nuevoSaldo = mapearTransaccionASaldo(tx);
                em.persist(nuevoSaldo);
            }
        });
    }

    /**
     * Método privado que convierte una TransaccionEntity en una SaldoEntity.
     * Esto limpia el método principal y centraliza la lógica de mapeo.
     *
     * @param tx La transacción de origen.
     * @return La nueva entidad de saldo lista para ser persistida.
     */
    private SaldoEntity mapearTransaccionASaldo(TransaccionEntity tx) {
        SaldoEntity nuevoSaldo = new SaldoEntity();

        // --- CAMPOS DE RELACIÓN Y BÁSICOS ---
        nuevoSaldo.setEmpresa(tx.getEmpresa());
        nuevoSaldo.setCustodio(tx.getCustodio());
        nuevoSaldo.setInstrumento(tx.getInstrumento());
        nuevoSaldo.setCuenta(tx.getCuenta());
        nuevoSaldo.setFecha(tx.getFecha());

        // --- CAMPOS DE CANTIDADES Y VALORES ---
        nuevoSaldo.setCantidad(tx.getCantidad());
        nuevoSaldo.setCantGarantia(BigDecimal.ZERO);
        nuevoSaldo.setCantLibre(BigDecimal.ZERO);
        nuevoSaldo.setCantPlazo(BigDecimal.ZERO);
        nuevoSaldo.setCantVc(BigDecimal.ZERO);
        nuevoSaldo.setPrecio(tx.getPrecio());
        nuevoSaldo.setMontoClp(tx.getMontoClp());
        nuevoSaldo.setMontoUsd(BigDecimal.ZERO);
        nuevoSaldo.setMoneda(tx.getMoneda());

        nuevoSaldo.setCreadoPor("CARGA_INICIAL");
        nuevoSaldo.setFechaCreacion(LocalDate.now());

        return nuevoSaldo;
    }
}
