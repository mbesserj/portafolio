package com.serv.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import com.model.utiles.LibraryInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Template para manejo de transacciones con diferentes niveles de control.
 * Alternativa más explícita al AbstractRepository para casos complejos.
 */
public class TransactionTemplate {

    private static final Logger logger = LoggerFactory.getLogger(TransactionTemplate.class);

    /**
     * Ejecuta una operación en transacción con manejo automático de rollback.
     */
    public static <T> T execute(Function<EntityManager, T> operation) {
        EntityManager em = null;
        EntityTransaction tx = null;

        try {
            em = LibraryInitializer.getEntityManager();
            tx = em.getTransaction();
            tx.begin();

            T result = operation.apply(em);

            tx.commit();
            return result;

        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                    logger.debug("Transacción revertida debido a error: {}", e.getMessage());
                } catch (Exception rollbackEx) {
                    logger.error("Error durante rollback", rollbackEx);
                    // Agregar el error de rollback como suppressed
                    e.addSuppressed(rollbackEx);
                }
            }
            throw new TransactionException("Error en transacción", e);

        } finally {
            if (em != null && em.isOpen()) {
                try {
                    em.close();
                } catch (Exception closeEx) {
                    logger.warn("Error al cerrar EntityManager", closeEx);
                }
            }
        }
    }

    /**
     * Ejecuta una operación en transacción sin valor de retorno.
     */
    public static void execute(Consumer<EntityManager> operation) {
        execute(em -> {
            operation.accept(em);
            return null;
        });
    }

    /**
     * Ejecuta una operación con control manual de transacción.
     * Útil cuando se necesita control granular sobre commit/rollback.
     */
    public static <T> T executeWithManualControl(ManualTransactionOperation<T> operation) {
        EntityManager em = null;
        EntityTransaction tx = null;

        try {
            em = LibraryInitializer.getEntityManager();
            tx = em.getTransaction();
            tx.begin();

            TransactionControl control = new TransactionControl(tx);
            T result = operation.execute(em, control);

            // Solo hacer commit si no se ha hecho rollback manual
            if (tx.isActive() && !control.isRolledBack()) {
                tx.commit();
            }

            return result;

        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (Exception rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
            }
            throw new TransactionException("Error en transacción con control manual", e);

        } finally {
            if (em != null && em.isOpen()) {
                try {
                    em.close();
                } catch (Exception closeEx) {
                    logger.warn("Error al cerrar EntityManager", closeEx);
                }
            }
        }
    }

    /**
     * Interfaz para operaciones con control manual de transacción.
     */
    @FunctionalInterface
    public interface ManualTransactionOperation<T> {
        T execute(EntityManager em, TransactionControl control) throws Exception;
    }

    /**
     * Clase que permite control manual de la transacción.
     */
    public static class TransactionControl {
        private final EntityTransaction transaction;
        private boolean rolledBack = false;

        TransactionControl(EntityTransaction transaction) {
            this.transaction = transaction;
        }

        public void rollback() {
            if (transaction.isActive()) {
                transaction.rollback();
                rolledBack = true;
                logger.debug("Rollback manual realizado");
            }
        }

        public void commit() {
            if (transaction.isActive() && !rolledBack) {
                transaction.commit();
                logger.debug("Commit manual realizado");
            }
        }

        public boolean isActive() {
            return transaction.isActive();
        }

        public boolean isRolledBack() {
            return rolledBack;
        }
    }

    /**
     * Excepción específica para errores de transacción.
     */
    public static class TransactionException extends RuntimeException {
        public TransactionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}