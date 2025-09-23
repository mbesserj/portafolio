package com.app.dao;

import com.app.entities.PerfilEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

public class PerfilDao extends AbstractJpaDao<PerfilEntity, Long> {

    public PerfilDao(EntityManager entityManager) {
        super(entityManager, PerfilEntity.class);
    }

    /**
     * Busca una entidad Perfil por su nombre único.
     * @param name El nombre del perfil a buscar (ej: "ADMINISTRADOR").
     * @return La entidad PerfilEntity si se encuentra, o null si no existe.
     */
    public PerfilEntity findByName(String name) {
        try {
            TypedQuery<PerfilEntity> query = entityManager.createQuery(
                "SELECT p FROM PerfilEntity p WHERE p.perfil = :name", PerfilEntity.class);
            
            query.setParameter("name", name);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}