package com.app.service;

import com.app.utiles.LibraryInitializer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class AbstractRepository {
    protected static final Logger logger = LoggerFactory.getLogger(AbstractRepository.class);
    
    /**
     * Ejecuta una operación con EntityManager gestionando automáticamente el ciclo de vida.
     */
    protected <T> T executeWithEntityManager(Function<EntityManager, T> action) {
        EntityManager em = null;
        try {
            em = LibraryInitializer.getEntityManager();
            return action.apply(em);
        } catch (Exception e) {
            logger.error("Error al ejecutar operación en servicio: {}", this.getClass().getSimpleName(), e);
            throw new RuntimeException("Error al ejecutar la operación: " + e.getMessage(), e);
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }
    
    /**
     * Ejecuta una operación dentro de una transacción con retorno.
     */
    protected <T> T executeInTransaction(Function<EntityManager, T> work) {
        return executeWithEntityManager(em -> {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                T result = work.apply(em);
                tx.commit();
                return result;
            } catch (Exception e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                throw e;
            }
        });
    }
    
    /**
     * Ejecuta una operación dentro de una transacción sin retorno.
     */
    protected void executeInTransaction(Consumer<EntityManager> work) {
        executeInTransaction(em -> {
            work.accept(em);
            return null;
        });
    }
    
    /**
     * Alias para operaciones de solo lectura.
     */
    protected <T> T executeReadOnly(Function<EntityManager, T> work) {
        return executeWithEntityManager(work);
    }
    
    /**
     * Ejecuta una función java.util
     */    
    protected <R> R executeInTransactionAndReturn(java.util.function.Function<EntityManager, R> func) {
        return executeWithEntityManager(em -> {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                R result = func.apply(em);
                tx.commit();
                return result;
            } catch (Exception e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                throw e;
            }
        });
    }
}