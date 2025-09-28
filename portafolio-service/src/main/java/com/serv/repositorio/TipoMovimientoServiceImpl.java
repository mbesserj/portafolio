package com.serv.repositorio;

import com.model.interfaces.AbstractRepository;
import com.model.dto.TipoMovimientoEstado;
import com.model.entities.MovimientoContableEntity;
import com.model.entities.TipoMovimientoEntity;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;
import com.model.interfaces.TipoMovimiento;

public class TipoMovimientoServiceImpl extends AbstractRepository implements TipoMovimiento {

    public TipoMovimientoServiceImpl() {
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