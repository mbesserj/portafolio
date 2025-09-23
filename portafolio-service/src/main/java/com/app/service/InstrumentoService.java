package com.app.service;

import com.app.dao.InstrumentoDao;
import com.app.dao.ProductoDao;
import com.app.entities.CustodioEntity;
import com.app.entities.EmpresaEntity;
import com.app.entities.InstrumentoEntity;
import com.app.entities.ProductoEntity;
import com.app.entities.TransaccionEntity;
import com.app.utiles.LibraryInitializer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstrumentoService {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentoService.class);

    /**
     * Obtiene una lista de TODAS las entidades de instrumentos disponibles,
     * cargando de forma proactiva la entidad Producto asociada.
     */
    public List<InstrumentoEntity> obtenerTodos() {
        EntityManager em = null;
        try {
            em = LibraryInitializer.getEntityManager();
            String hql = "SELECT i FROM InstrumentoEntity i JOIN FETCH i.producto ORDER BY i.instrumentoNemo ASC";
            return em.createQuery(hql, InstrumentoEntity.class).getResultList();
        } catch (Exception e) {
            logger.error("Error al obtener la lista completa de instrumentos.", e);
            return Collections.emptyList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    /**
     * Obtiene una lista de entidades de Instrumento que han tenido
     * transacciones para un custodio y una empresa específicos.
     */
    public List<InstrumentoEntity> obtenerInstrumentosPorCustodioYEmpresa(String nombreCustodio, String razonSocial) {
        EntityManager entityManager = null;
        try {
            entityManager = LibraryInitializer.getEntityManager();

            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            // CAMBIO 1: El tipo de la query ahora es InstrumentoEntity
            CriteriaQuery<InstrumentoEntity> query = cb.createQuery(InstrumentoEntity.class);
            Root<TransaccionEntity> transaccion = query.from(TransaccionEntity.class);

            Join<TransaccionEntity, InstrumentoEntity> instrumento = transaccion.join("instrumento");
            Join<TransaccionEntity, CustodioEntity> custodio = transaccion.join("custodio");
            Join<TransaccionEntity, EmpresaEntity> empresa = transaccion.join("empresa");

            // CAMBIO 2: Seleccionamos la entidad completa del instrumento
            query.select(instrumento).distinct(true)
                    .where(
                            cb.equal(custodio.get("nombreCustodio"), nombreCustodio),
                            cb.equal(empresa.get("razonSocial"), razonSocial)
                    )
                    // Opcional pero recomendado: Ordenar el resultado
                    .orderBy(cb.asc(instrumento.get("instrumentoNemo")));

            return entityManager.createQuery(query).getResultList();

        } catch (Exception e) {
            logger.error("Error al obtener instrumentos para [Empresa: {}, Custodio: {}]",
                    razonSocial, nombreCustodio, e);
            return Collections.emptyList();
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }

    /**
     * Busca un InstrumentoEntity por su ID.
     *
     * @param id El ID del instrumento a buscar.
     * @return El InstrumentoEntity encontrado o null si no existe.
     */
    public InstrumentoEntity obtenerPorId(Long id) {
        EntityManager em = null;
        try {
            em = LibraryInitializer.getEntityManager();
            return em.find(InstrumentoEntity.class, id);
        } catch (Exception e) {
            logger.error("Error al buscar instrumento con ID: {}", id, e);
            return null;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<InstrumentoEntity> obtenerInstrumentosConTransacciones(Long empresaId, Long custodioId, String cuenta) {
        EntityManager em = null;
        if (empresaId == null || custodioId == null || cuenta == null) {
            return Collections.emptyList();
        }
        try {
            em = LibraryInitializer.getEntityManager();
            String jpql = """
            SELECT DISTINCT t.instrumento FROM TransaccionEntity t
            WHERE t.empresa.id = :empresaId
              AND t.custodio.id = :custodioId
              AND t.cuenta = :cuenta
            ORDER BY t.instrumento.instrumentoNemo ASC
            """;
            TypedQuery<InstrumentoEntity> query = em.createQuery(jpql, InstrumentoEntity.class);
            query.setParameter("empresaId", empresaId);
            query.setParameter("custodioId", custodioId);
            query.setParameter("cuenta", cuenta);
            return query.getResultList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    /**
     * Busca un instrumento por su Nemo. Si no existe, delega su creación al
     * DAO. Este método actúa como un puente entre la lógica de la aplicación y
     * el acceso a datos del DAO.
     *
     * @param nemo El nemo del instrumento.
     * @param nombre El nombre completo del instrumento.
     * @return La entidad del instrumento, ya sea encontrada o recién creada.
     */
    public InstrumentoEntity buscarOCrear(String nemo, String nombre) {
        EntityManager em = LibraryInitializer.getEntityManager();
        try {
            // La transacción se maneja dentro del DAO, pero necesitamos el EntityManager
            // para instanciar el DAO y buscar el Producto.
            
            ProductoEntity productoDefecto = em.find(ProductoEntity.class, 1L);
            
            InstrumentoDao dao = new InstrumentoDao(em, new ProductoDao(em));

            if (productoDefecto == null) {
                throw new IllegalStateException("No se encontró un Producto por defecto para crear el instrumento.");
            }

            // Delegamos la lógica de buscar o crear al DAO, que ya está bien implementada.
            return dao.findOrCreateByInstrumento(nemo, nombre, productoDefecto);

        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }
}
