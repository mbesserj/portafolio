package com.app.service;

import com.app.dao.EmpresaDao;
import com.app.entities.EmpresaEntity;
import com.app.utiles.LibraryInitializer;
import jakarta.persistence.EntityManager;
import java.util.List;

public class EmpresaService {

    public List<EmpresaEntity> obtenerTodas() {
        EntityManager em = LibraryInitializer.getEntityManager();
        try {
            return new EmpresaDao(em).findAll();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<EmpresaEntity> obtenerEmpresasConTransacciones() {
        EntityManager em = null;
        try {
            em = LibraryInitializer.getEntityManager();
            String jpql = "SELECT DISTINCT t.empresa FROM TransaccionEntity t ORDER BY t.empresa.razonSocial ASC";
            return em.createQuery(jpql, EmpresaEntity.class).getResultList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public EmpresaEntity obtenerPorId(Long id) {
        EntityManager em = null;
        try {
            em = LibraryInitializer.getEntityManager();
            return em.find(EmpresaEntity.class, id);
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }
}
