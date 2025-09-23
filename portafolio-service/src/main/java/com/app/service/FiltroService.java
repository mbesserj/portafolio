
package com.app.service;

import com.app.entities.*;
import com.app.utiles.LibraryInitializer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.*;

public class FiltroService {

    public List<EmpresaEntity> obtenerEmpresasConTransacciones() {
        EntityManager em = null;
        try {
            em = LibraryInitializer.getEntityManager();
            String jpql = "SELECT DISTINCT t.empresa FROM TransaccionEntity t ORDER BY t.empresa.razonSocial ASC";
            return em.createQuery(jpql, EmpresaEntity.class).getResultList();
        } finally {
            if (em != null) em.close();
        }
    }

    public List<CustodioEntity> obtenerCustodiosConTransacciones(Long empresaId) {
        EntityManager em = null;
        if (empresaId == null) return Collections.emptyList();
        try {
            em = LibraryInitializer.getEntityManager();
            String jpql = "SELECT DISTINCT t.custodio FROM TransaccionEntity t WHERE t.empresa.id = :empresaId ORDER BY t.custodio.nombreCustodio ASC";
            TypedQuery<CustodioEntity> query = em.createQuery(jpql, CustodioEntity.class);
            query.setParameter("empresaId", empresaId);
            return query.getResultList();
        } finally {
            if (em != null) em.close();
        }
    }

    public List<String> obtenerCuentasConTransacciones(Long empresaId, Long custodioId) {
         EntityManager em = null;
        if (empresaId == null || custodioId == null) return Collections.emptyList();
        try {
            em = LibraryInitializer.getEntityManager();
            String jpql = "SELECT DISTINCT t.cuenta FROM TransaccionEntity t WHERE t.empresa.id = :empresaId AND t.custodio.id = :custodioId AND t.cuenta IS NOT NULL ORDER BY t.cuenta ASC";
            TypedQuery<String> query = em.createQuery(jpql, String.class);
            query.setParameter("empresaId", empresaId);
            query.setParameter("custodioId", custodioId);
            return query.getResultList();
        } finally {
            if (em != null) em.close();
        }
    }

    public List<InstrumentoEntity> obtenerInstrumentosConTransacciones(Long empresaId, Long custodioId, String cuenta) {
        EntityManager em = null;
        if (empresaId == null || custodioId == null || cuenta == null) return Collections.emptyList();
        try {
            em = LibraryInitializer.getEntityManager();
            String jpql = """
                SELECT DISTINCT t.instrumento FROM TransaccionEntity t
                WHERE t.empresa.id = :empresaId AND t.custodio.id = :custodioId AND t.cuenta = :cuenta
                ORDER BY t.instrumento.instrumentoNemo ASC
                """;
            TypedQuery<InstrumentoEntity> query = em.createQuery(jpql, InstrumentoEntity.class);
            query.setParameter("empresaId", empresaId);
            query.setParameter("custodioId", custodioId);
            query.setParameter("cuenta", cuenta);
            return query.getResultList();
        } finally {
            if (em != null) em.close();
        }
    }
}