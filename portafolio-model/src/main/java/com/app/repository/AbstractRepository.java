package com.app.repository;

import com.app.utiles.LibraryInitializer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class AbstractRepository {

    protected final EntityManager em;

    public AbstractRepository() {
        this.em = LibraryInitializer.getEntityManager();
    }

    /**
     * Ejecuta una operación genérica con el EntityManager y retorna un valor.
     */
    protected <T> T execute(Function<EntityManager, T> action) {
        try {
            return action.apply(this.em);
        } catch (Exception e) {
            throw new RuntimeException("Error al ejecutar la operación en el repositorio.", e);
        }
    }

    /**
     * Ejecuta un bloque de código dentro de una transacción sin retorno.
     */
    protected void executeInTransaction(Consumer<EntityManager> work) {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            work.accept(em);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Error en transacción", e);
        }
    }

    /**
     * Ejecuta un bloque de código dentro de una transacción con retorno.
     */
    protected <T> T executeInTransaction(Function<EntityManager, T> work) {
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
            throw new RuntimeException("Error en transacción", e);
        }
    }

    /**
     * Alias para operaciones de solo lectura.
     */
    protected <T> T executeReadOnly(Function<EntityManager, T> work) {
        return execute(work);
    }
}
