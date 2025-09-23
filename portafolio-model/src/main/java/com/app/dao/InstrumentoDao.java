package com.app.dao;

import com.app.entities.InstrumentoEntity;
import com.app.entities.ProductoEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DAO para gestionar la lógica de acceso a datos de la entidad Instrumento.
 * Se especializa en encontrar o crear instrumentos, asegurando que cada uno
 * esté asociado a un producto y manejando casos de NEMO nulos.
 */
public class InstrumentoDao extends AbstractJpaDao<InstrumentoEntity, Long> {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentoDao.class);
    private final ProductoDao productoDao;
    
    // Constantes para los valores por defecto de instrumentos de caja/efectivo.
    private static final String NEMO_CAJA_DEFAULT = "CAJA";
    private static final String NOMBRE_CAJA_DEFAULT = "Efectivo y Equivalentes";

    public InstrumentoDao(EntityManager entityManager, ProductoDao productoDao) {
        super(entityManager, InstrumentoEntity.class);
        this.productoDao = productoDao;
    }

    /**
     * Busca un instrumento por su NEMO. Si no lo encuentra, lo crea.
     * Si el NEMO es nulo o vacío, lo asocia a un instrumento genérico de "CAJA".
     *
     * @param nemo El NEMO (ticker) del instrumento.
     * @param nombre El nombre completo o descripción del instrumento.
     * @param producto El producto sugerido (será ignorado si se crea un nuevo instrumento).
     * @return La entidad InstrumentoEntity encontrada o recién creada. Nunca devuelve null.
     */
    public InstrumentoEntity findOrCreateByInstrumento(String nemo, String nombre, ProductoEntity producto) {
        
        String nemoBusqueda;
        String nombreBusqueda;

        // --- LÓGICA MEJORADA PARA MANEJAR NEMO VACÍO ---
        if (nemo == null || nemo.trim().isEmpty()) {
            logger.warn("NEMO nulo o vacío detectado. Se asignará al instrumento por defecto '{}'.", NEMO_CAJA_DEFAULT);
            nemoBusqueda = NEMO_CAJA_DEFAULT;
            nombreBusqueda = NOMBRE_CAJA_DEFAULT;
        } else {
            nemoBusqueda = nemo.trim();
            // Si el nombre viene vacío, usamos el NEMO como nombre por defecto.
            nombreBusqueda = (nombre == null || nombre.trim().isEmpty()) ? nemoBusqueda : nombre.trim();
        }

        try {
            // Buscamos el instrumento usando el NEMO (que ahora siempre tiene valor).
            TypedQuery<InstrumentoEntity> query = entityManager.createQuery(
                    "SELECT i FROM InstrumentoEntity i WHERE i.instrumentoNemo = :nemo", InstrumentoEntity.class);
            query.setParameter("nemo", nemoBusqueda);
            return query.getSingleResult();

        } catch (NoResultException e) {
            // Si no se encuentra, lo creamos.
            logger.info("Instrumento con NEMO '{}' no encontrado. Creando uno nuevo...", nemoBusqueda);

            try {
                // Obtenemos el producto por defecto "Sin producto definido".
                ProductoEntity productoPorDefinir = productoDao.findOrCreateByProducto("Sin producto definido");

                InstrumentoEntity nuevoInstrumento = new InstrumentoEntity();
                nuevoInstrumento.setInstrumentoNemo(nemoBusqueda);
                nuevoInstrumento.setInstrumentoNombre(nombreBusqueda); 
                nuevoInstrumento.setProducto(productoPorDefinir);
                
                nuevoInstrumento.setCreadoPor("sistema");
                nuevoInstrumento.setModificadoPor("sistema");
                nuevoInstrumento.setFechaCreacion(LocalDate.now());
                nuevoInstrumento.setFechaModificacion(LocalDate.now());

                create(nuevoInstrumento);
                logger.info("Instrumento '{}' creado exitosamente.", nemoBusqueda);
                
                return nuevoInstrumento;

            } catch (PersistenceException pe) {
                // Manejo de condición de carrera: si otro hilo lo creó, lo volvemos a buscar.
                logger.warn("Fallo al crear el instrumento '{}', probablemente ya existe. Reintentando búsqueda...", nemoBusqueda);
                TypedQuery<InstrumentoEntity> query = entityManager.createQuery(
                    "SELECT i FROM InstrumentoEntity i WHERE i.instrumentoNemo = :nemo", InstrumentoEntity.class);
                query.setParameter("nemo", nemoBusqueda);
                return query.getSingleResult();
            }
        }
    }
}
