package com.app.dao;

import com.app.entities.CustodioEntity;
import com.app.entities.EmpresaEntity;
import com.app.entities.InstrumentoEntity;
import com.app.entities.SaldosDiariosEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * DAO para gestionar la entidad SaldosDiariosEntity.
 * Proporciona métodos para crear, actualizar y consultar registros de saldos diarios.
 */
public class SaldosDiariosDao {
    private final EntityManager em;

    public SaldosDiariosDao(EntityManager em) { 
        this.em = em; 
    }

    /**
     * Inserta o actualiza un registro de saldo diario (lógica "UPSERT").
     */
    public void upsert(SaldosDiariosEntity saldoDelDia) {
        Optional<SaldosDiariosEntity> existente = findByBusinessKey(
                saldoDelDia.getFecha(),
                saldoDelDia.getEmpresa(),
                saldoDelDia.getCustodio(),
                saldoDelDia.getInstrumento(),
                saldoDelDia.getCuenta()
        );

        if (existente.isPresent()) {
            SaldosDiariosEntity aActualizar = existente.get();
            aActualizar.setSaldoCantidad(saldoDelDia.getSaldoCantidad());
            aActualizar.setSaldoValor(saldoDelDia.getSaldoValor());
            em.merge(aActualizar);
        } else {
            em.persist(saldoDelDia);
        }
    }

    /**
     * Busca un registro de saldo diario por su clave de negocio única.
     */
    private Optional<SaldosDiariosEntity> findByBusinessKey(LocalDate fecha, EmpresaEntity e, CustodioEntity c, InstrumentoEntity i, String cuenta) {
        String hql = """
            SELECT s FROM SaldosDiariosEntity s
            WHERE s.fecha = :fecha AND s.empresa = :empresa AND s.custodio = :custodio 
            AND s.instrumento = :instrumento AND s.cuenta = :cuenta
            """;
        try {
            return Optional.of(em.createQuery(hql, SaldosDiariosEntity.class)
                .setParameter("fecha", fecha)
                .setParameter("empresa", e)
                .setParameter("custodio", c)
                .setParameter("instrumento", i)
                .setParameter("cuenta", cuenta)
                .getSingleResult());
        } catch (NoResultException ex) {
            return Optional.empty();
        }
    }

    /**
     * Busca el último saldo registrado ANTES de una fecha específica para un grupo.
     */
    public Optional<SaldosDiariosEntity> findLastBeforeDate(EmpresaEntity e, CustodioEntity c, InstrumentoEntity i, String cuenta, LocalDate date) {
        String hql = """
            SELECT s FROM SaldosDiariosEntity s
            WHERE s.empresa = :empresa AND s.custodio = :custodio AND s.instrumento = :instrumento
            AND s.cuenta = :cuenta AND s.fecha < :date
            ORDER BY s.fecha DESC
            """;
        try {
            return Optional.of(em.createQuery(hql, SaldosDiariosEntity.class)
                .setParameter("empresa", e)
                .setParameter("custodio", c)
                .setParameter("instrumento", i)
                .setParameter("cuenta", cuenta)
                .setParameter("date", date)
                .setMaxResults(1)
                .getSingleResult());
        } catch (NoResultException ex) {
            return Optional.empty();
        }
    }

    /**
     * NUEVO MÉTODO AÑADIDO:
     * Obtiene todos los saldos diarios para un grupo dentro de un rango de fechas.
     * Esencial para la optimización de rendimiento del FifoCostingEngine.
     */
    public List<SaldosDiariosEntity> findAllByGroupAndDateRange(EmpresaEntity e, CustodioEntity c, InstrumentoEntity i, String cuenta, LocalDate startDate, LocalDate endDate) {
        String hql = """
            SELECT s FROM SaldosDiariosEntity s
            WHERE s.empresa = :empresa AND s.custodio = :custodio AND s.instrumento = :instrumento
            AND s.cuenta = :cuenta AND s.fecha BETWEEN :startDate AND :endDate
            """;
        return em.createQuery(hql, SaldosDiariosEntity.class)
                .setParameter("empresa", e)
                .setParameter("custodio", c)
                .setParameter("instrumento", i)
                .setParameter("cuenta", cuenta)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getResultList();
    }
}