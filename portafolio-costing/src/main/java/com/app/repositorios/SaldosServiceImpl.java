package com.app.repositorios;

import com.app.interfaz.AbstractRepository;
import com.app.dto.ResumenSaldoDto;
import com.app.entities.CustodioEntity;
import com.app.entities.EmpresaEntity;
import com.app.entities.InstrumentoEntity;
import com.app.entities.SaldoEntity;
import com.app.entities.SaldoKardexEntity;
import com.app.entities.SaldosDiariosEntity;
import jakarta.persistence.NoResultException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import com.app.interfaz.SaldoApiInterfaz;

/**
 * Implementación del repositorio para acceder a los datos de Saldos Diarios.
 */
public class SaldosServiceImpl extends AbstractRepository implements SaldoApiInterfaz {

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
}