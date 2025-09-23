package com.app.service;

import com.app.dao.CustodioDao;
import com.app.entities.CustodioEntity;
import com.app.entities.EmpresaEntity;
import com.app.utiles.LibraryInitializer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustodioService {

    private static final Logger logger = LoggerFactory.getLogger(CustodioService.class);

    public List<CustodioEntity> obtenerTodos() {
        EntityManager em = null;
        try {
            em = LibraryInitializer.getEntityManager();
            return new CustodioDao(em).findAll();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    /**
     * MÉTODO IMPLEMENTADO: Obtiene una lista de custodios únicos que tienen
     * transacciones asociadas a una empresa específica.
     *
     * @param empresaId El ID de la empresa a filtrar.
     * @return Una lista de entidades CustodioEntity.
     */
    public List<CustodioEntity> obtenerCustodiosPorEmpresa(Long empresaId) {
        EntityManager em = null;
        if (empresaId == null) {
            return Collections.emptyList();
        }
        try {
            em = LibraryInitializer.getEntityManager();

            String jpql = "SELECT e FROM EmpresaEntity e LEFT JOIN FETCH e.custodios WHERE e.id = :empresaId";

            TypedQuery<EmpresaEntity> query = em.createQuery(jpql, EmpresaEntity.class);
            query.setParameter("empresaId", empresaId);

            EmpresaEntity empresa = query.getSingleResult();

            if (empresa != null && empresa.getCustodios() != null) {
                List<CustodioEntity> custodiosList = new ArrayList<>(empresa.getCustodios());
                custodiosList.sort(Comparator.comparing(CustodioEntity::getNombreCustodio));
                return custodiosList;
            }

            return Collections.emptyList();

        } catch (Exception e) {
            logger.error("Error al obtener custodios para la empresa con ID: {}", empresaId, e);
            return Collections.emptyList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<String> obtenerCuentasPorCustodioYEmpresa(Long custodioId, Long empresaId) {
        EntityManager em = null;
        if (custodioId == null || empresaId == null) {
            return Collections.emptyList();
        }
        try {
            em = LibraryInitializer.getEntityManager();

            // CAMBIO: La consulta JPQL ahora selecciona "c.cuenta"
            String jpql = """
            SELECT c.cuenta 
            FROM CuentaEntity c
            WHERE c.custodio.id = :custodioId 
              AND c.empresa.id = :empresaId
            ORDER BY c.cuenta ASC
            """;

            TypedQuery<String> query = em.createQuery(jpql, String.class);
            query.setParameter("custodioId", custodioId);
            query.setParameter("empresaId", empresaId);

            return query.getResultList();

        } catch (Exception e) {
            logger.error("Error al obtener cuentas para custodio {} y empresa {}", custodioId, empresaId, e);
            return Collections.emptyList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public CustodioEntity obtenerPorId(Long id) {
        EntityManager em = null;
        try {
            em = LibraryInitializer.getEntityManager();
            return em.find(CustodioEntity.class, id);
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }
}