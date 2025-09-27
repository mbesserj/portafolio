package com.app.service;

import com.app.dao.InstrumentoDao;
import com.app.dao.ProductoDao;
import com.app.entities.CustodioEntity;
import com.app.entities.EmpresaEntity;
import com.app.entities.InstrumentoEntity;
import com.app.entities.ProductoEntity;
import com.app.entities.TransaccionEntity;
import com.app.interfaces.AbstractRepository;
import com.app.sql.QueryRepository;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstrumentoService extends AbstractRepository {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentoService.class);

    /**
     * Constructor.
     */
    public InstrumentoService() {
        super();
    }

    public List<InstrumentoEntity> obtenerTodos() {
        return executeReadOnly(em -> {
            try {
                String sql_instrumento_todos = QueryRepository.getInstrumentoQuery(QueryRepository.IntrumentoQueries.INSTRUMENTO_TODOS);
                return em.createQuery(sql_instrumento_todos, InstrumentoEntity.class).getResultList();
            } catch (Exception e) {
                logger.error("Error al obtener la lista completa de instrumentos.", e);
                return Collections.emptyList();
            }
        });
    }

    public List<InstrumentoEntity> obtenerInstrumentosPorCustodioYEmpresa(String nombreCustodio, String razonSocial) {
        return executeReadOnly(em -> {
            try {
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaQuery<InstrumentoEntity> query = cb.createQuery(InstrumentoEntity.class);
                Root<TransaccionEntity> transaccion = query.from(TransaccionEntity.class);

                Join<TransaccionEntity, InstrumentoEntity> instrumento = transaccion.join("instrumento");
                Join<TransaccionEntity, CustodioEntity> custodio = transaccion.join("custodio");
                Join<TransaccionEntity, EmpresaEntity> empresa = transaccion.join("empresa");

                query.select(instrumento).distinct(true)
                        .where(
                                cb.equal(custodio.get("nombreCustodio"), nombreCustodio),
                                cb.equal(empresa.get("razonSocial"), razonSocial)
                        )
                        .orderBy(cb.asc(instrumento.get("instrumentoNemo")));

                return em.createQuery(query).getResultList();
            } catch (Exception e) {
                logger.error("Error al obtener instrumentos para [Empresa: {}, Custodio: {}]",
                        razonSocial, nombreCustodio, e);
                return Collections.emptyList();
            }
        });
    }

    public InstrumentoEntity obtenerPorId(Long id) {
        return executeReadOnly(em -> {
            try {
                return em.find(InstrumentoEntity.class, id);
            } catch (Exception e) {
                logger.error("Error al buscar instrumento con ID: {}", id, e);
                return null;
            }
        });
    }

    public List<InstrumentoEntity> obtenerInstrumentosConTransacciones(Long empresaId, Long custodioId, String cuenta) {
        if (empresaId == null || custodioId == null || cuenta == null) {
            return Collections.emptyList();
        }

        return executeReadOnly(em -> {
            String sql_instrumento_por_empresa_custodio = QueryRepository.getInstrumentoQuery(QueryRepository.IntrumentoQueries.INSTRUMENTO_POR_CUSTODIO_Y_EMPRESA_QUERY);
            TypedQuery<InstrumentoEntity> query = em.createQuery(sql_instrumento_por_empresa_custodio, InstrumentoEntity.class);
            query.setParameter("empresaId", empresaId);
            query.setParameter("custodioId", custodioId);
            query.setParameter("cuenta", cuenta);
            return query.getResultList();
        });
    }

    public InstrumentoEntity buscarOCrear(String nemo, String nombre) {
        return executeInTransaction(em -> {
            ProductoEntity productoDefecto = em.find(ProductoEntity.class, 1L);

            if (productoDefecto == null) {
                throw new IllegalStateException("No se encontró un Producto por defecto para crear el instrumento.");
            }

            InstrumentoDao dao = new InstrumentoDao(em, new ProductoDao(em));
            return dao.findOrCreateByInstrumento(nemo, nombre, productoDefecto);
        });
    }
}
