package com.app.dao;

import com.app.entities.UsuarioEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

public class UsuarioDao extends AbstractJpaDao<UsuarioEntity, Long> {

    public UsuarioDao(EntityManager entityManager) {
        super(entityManager, UsuarioEntity.class);
    }
    
    public UsuarioEntity findByUsername(String username) {
        try {
            TypedQuery<UsuarioEntity> query = entityManager.createQuery(
                "SELECT u FROM UsuarioEntity u WHERE u.usuario = :username", UsuarioEntity.class);            
            
            query.setParameter("username", username);     
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}