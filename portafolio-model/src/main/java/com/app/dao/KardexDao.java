package com.app.dao;

import com.app.entities.KardexEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class KardexDao extends AbstractJpaDao<KardexEntity, Long> {

    public KardexDao(EntityManager entityManager) {
        super(entityManager, KardexEntity.class);
    }

    public Optional<KardexEntity> findLastByGroup(Long empresaId, String cuenta, Long custodioId, Long instrumentoId) {
        String hql = """
            SELECT k FROM KardexEntity k
            WHERE k.empresa.id = :empresaId
              AND k.cuenta = :cuenta
              AND k.custodio.id = :custodioId
              AND k.instrumento.id = :instrumentoId
            ORDER BY k.fechaTransaccion DESC, k.id DESC
            """;
        try {
            KardexEntity lastKardex = entityManager.createQuery(hql, KardexEntity.class)
                    .setParameter("empresaId", empresaId)
                    .setParameter("cuenta", cuenta)
                    .setParameter("custodioId", custodioId)
                    .setParameter("instrumentoId", instrumentoId)
                    .setMaxResults(1)
                    .getSingleResult();
            return Optional.of(lastKardex);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public List<KardexEntity> findByGroupAndDateRange(Long empId, Long custId, Long instId, String cta, LocalDate start, LocalDate end) {
        String hql = """
        SELECT k FROM KardexEntity k
        WHERE k.empresa.id = :empId AND k.custodio.id = :custId AND k.instrumento.id = :instId
        AND k.cuenta = :cta AND k.fechaTransaccion BETWEEN :start AND :end
        ORDER BY k.fechaTransaccion, k.id
        """;
        return entityManager.createQuery(hql, KardexEntity.class)
                .setParameter("empId", empId)
                .setParameter("custId", custId)
                .setParameter("instId", instId)
                .setParameter("cta", cta)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();
    }

}