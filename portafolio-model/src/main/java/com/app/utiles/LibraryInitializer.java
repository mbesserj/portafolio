package com.app.utiles;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clase de utilidad estática para gestionar el ciclo de vida de la base de
 * datos para la librería. La aplicación que consuma esta librería debe llamar a
 * 'init()' al inicio y a 'shutdown()' al final.
 */
public final class LibraryInitializer {

    private static final Logger logger = LoggerFactory.getLogger(LibraryInitializer.class);
    private static EntityManagerFactory entityManagerFactory;
    private static final String PERSISTENCE_UNIT_NAME = "JpaUnit";

    private LibraryInitializer() {
        // Constructor privado para evitar la instanciación
    }

    /**
     * Inicializa la conexión a la base de datos.
     */
    public static void init() {
        if (entityManagerFactory == null) {
            try {
                entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
                logger.info("EntityManagerFactory inicializado con éxito.");

                // Inicialización de datos base (incluye verificación y creación de vistas)
                DataInitializer.inicializarDatosBase();

            } catch (Exception e) {
                logger.error("Error al inicializar el EntityManagerFactory.", e);
                throw new RuntimeException("No se pudo inicializar la base de datos.", e);
            }
        }
    }

    /**
     * Proporciona una nueva instancia de EntityManager. Cada hilo o unidad de
     * trabajo debe usar su propio EntityManager, que se cerrará una vez
     * finalizado su uso.
     *
     * @return Una nueva instancia de EntityManager.
     * @throws IllegalStateException Si el EntityManagerFactory no ha sido
     * inicializado.
     */
    public static EntityManager getEntityManager() {
        if (entityManagerFactory == null || !entityManagerFactory.isOpen()) {
            throw new IllegalStateException("El EntityManagerFactory no está inicializado o ya fue cerrado.");
        }
        return entityManagerFactory.createEntityManager();
    }

    /**
     * Cierra el EntityManagerFactory, liberando todos los recursos. Este método
     * debe ser llamado al cerrar la aplicación para evitar fugas de memoria.
     */
    public static void shutdown() {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            logger.info("Cerrando EntityManagerFactory.");
            entityManagerFactory.close();
        }
    }
}