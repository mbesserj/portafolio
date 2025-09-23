package com.app.repositorios;

import com.app.dto.TipoMovimientoEstado;
import com.app.entities.MovimientoContableEntity;
import com.app.entities.TipoMovimientoEntity;
import com.app.repository.AbstractRepository;
import com.app.repository.TipoMovimientoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;

public class TipoMovimientoRepositoryImpl extends AbstractRepository implements TipoMovimientoRepository {

    public TipoMovimientoRepositoryImpl() {
        super();
    }

    @Override
    public List<TipoMovimientoEstado> obtenerMovimientosConCriteria() {
        return execute(em -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<TipoMovimientoEstado> cq = cb.createQuery(TipoMovimientoEstado.class);
            Root<TipoMovimientoEntity> t = cq.from(TipoMovimientoEntity.class);
            Join<TipoMovimientoEntity, MovimientoContableEntity> tc = t.join("movimientoContable");

            cq.select(cb.construct(
                    TipoMovimientoEstado.class,
                    t.get("id"),
                    t.get("descripcion"),
                    t.get("tipoMovimiento"),
                    tc.get("tipoContable")
            ));
            cq.orderBy(cb.asc(tc.get("tipoContable")));

            TypedQuery<TipoMovimientoEstado> query = em.createQuery(cq);
            return query.getResultList();
        });
    }

    @Override
    public Optional<TipoMovimientoEntity> buscarPorNombre(String tipoMovimiento) {
        return execute(em -> {
            TypedQuery<TipoMovimientoEntity> query = em.createQuery(
                    "SELECT t FROM TipoMovimientoEntity t WHERE t.tipoMovimiento = :tipoMovimiento",
                    TipoMovimientoEntity.class
            );
            query.setParameter("tipoMovimiento", tipoMovimiento);
            return query.getResultList().stream().findFirst();
        });
    }

    @Override
    public List<TipoMovimientoEntity> obtenerTodosLosTipos() {
        return execute(em -> {
            TypedQuery<TipoMovimientoEntity> query = em.createQuery("SELECT t FROM TipoMovimientoEntity t", TipoMovimientoEntity.class);
            return query.getResultList();
        });
    }
}