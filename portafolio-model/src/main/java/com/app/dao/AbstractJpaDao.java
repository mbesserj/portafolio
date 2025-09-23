package com.app.dao;

import java.io.Serializable;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

public abstract class AbstractJpaDao<T, PK extends Serializable> {

    protected final EntityManager entityManager;
    private final Class<T> entityClass;

    public AbstractJpaDao(EntityManager entityManager, Class<T> entityClass) {
        this.entityManager = entityManager;
        this.entityClass = entityClass;
    }

    public T create(T entity) {
        entityManager.persist(entity);
        return entity;
    }

    public T update(T entity) {
        return entityManager.merge(entity);
    }

    public void remove(T entity) {
        entityManager.remove(entityManager.merge(entity));
    }

    public T findById(PK id) {
        return entityManager.find(entityClass, id);
    }
    
    public boolean exists(PK id) {
        try {
            entityManager.createQuery("SELECT 1 FROM " + entityClass.getSimpleName() + " WHERE id = :id")
                .setParameter("id", id)
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public List<T> findAll() {
        return entityManager.createQuery("SELECT e FROM " + entityClass.getSimpleName() + " e").getResultList();
    }
}