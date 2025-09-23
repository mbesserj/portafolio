
package com.app.dao;

import com.app.entities.CierreContableEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import java.util.Optional;

public class CierreContableDao extends AbstractJpaDao<CierreContableEntity, Long> {

    public CierreContableDao(EntityManager entityManager) {
        super(entityManager, CierreContableEntity.class);
    }

    /**
     * Busca un saldo de cierre para un ejercicio y una combinación específica.
     */
    public Optional<CierreContableEntity> findByEjercicioAndGrupo(int ejercicio, Long empresaId, Long custodioId, Long instrumentoId) {
        String jpql = """
            SELECT c FROM CierreContableEntity c
            WHERE c.ejercicio = :ejercicio
              AND c.empresa.id = :empresaId
              AND c.custodio.id = :custodioId
              AND c.instrumento.id = :instrumentoId
            """;
        TypedQuery<CierreContableEntity> query = entityManager.createQuery(jpql, CierreContableEntity.class);
        query.setParameter("ejercicio", ejercicio);
        query.setParameter("empresaId", empresaId);
        query.setParameter("custodioId", custodioId);
        query.setParameter("instrumentoId", instrumentoId);

        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
}