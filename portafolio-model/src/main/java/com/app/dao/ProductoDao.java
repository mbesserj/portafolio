package com.app.dao;

import com.app.entities.ProductoEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductoDao extends AbstractJpaDao<ProductoEntity, Long> {

    private static final Logger logger = LoggerFactory.getLogger(ProductoDao.class);

    public ProductoDao(EntityManager entityManager) {
        super(entityManager, ProductoEntity.class);
    }

    public ProductoEntity findOrCreateByProducto(String nombreProducto) {
        // Asignar "Sin producto" si el nombre es nulo o vacío
        if (nombreProducto == null || nombreProducto.trim().isEmpty()) {
            nombreProducto = "Sin producto definido";
        }

        try {
            TypedQuery<ProductoEntity> query = entityManager.createQuery(
                    "SELECT p FROM ProductoEntity p WHERE p.producto = :nombre", ProductoEntity.class);
            query.setParameter("nombre", nombreProducto);
            return query.getSingleResult();
        } catch (NoResultException e) {
            try {
                ProductoEntity nuevoProducto = new ProductoEntity();
                nuevoProducto.setProducto(nombreProducto);
                nuevoProducto.setDetalleProducto("Producto asignado automáticamente");
                nuevoProducto.setFechaCreacion(LocalDate.now());
                nuevoProducto.setFechaModificacion(LocalDate.now());
                nuevoProducto.setCreadoPor("sistema");
                nuevoProducto.setModificadoPor("sistema");

                create(nuevoProducto);
                return nuevoProducto;
            } catch (PersistenceException pe) {
                logger.warn("Fallo al crear el producto '{}'. Intentando buscar de nuevo...", nombreProducto);
                try {
                    TypedQuery<ProductoEntity> query = entityManager.createQuery(
                            "SELECT p FROM ProductoEntity p WHERE p.producto = :nombre", ProductoEntity.class);
                    query.setParameter("nombre", nombreProducto);
                    return query.getSingleResult();
                } catch (NoResultException e2) {
                    logger.error("No se pudo encontrar ni crear el producto '{}' después de un segundo intento.", nombreProducto);
                    throw new IllegalStateException("Fallo crítico en findOrCreateByProducto.", e2);
                }
            }
        }
    }
}