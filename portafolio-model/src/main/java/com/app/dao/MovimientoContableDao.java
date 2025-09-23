package com.app.dao;

import com.app.entities.MovimientoContableEntity;
import com.app.enums.TipoEnumsCosteo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import java.util.Optional;

public class MovimientoContableDao extends AbstractJpaDao<MovimientoContableEntity, Long> {

    public MovimientoContableDao(EntityManager entityManager) {
        super(entityManager, MovimientoContableEntity.class);
    }

    public MovimientoContableEntity buscarOCrearPorTipo(TipoEnumsCosteo tipo) {
        // 1. Llama al método que busca la entidad
        Optional<MovimientoContableEntity> entidadExistente = this.findByTipo(tipo);

        // 2. Si no existe, la crea
        return entidadExistente.orElseGet(() -> {
            MovimientoContableEntity nuevaEntidad = new MovimientoContableEntity();            
            nuevaEntidad.setTipoContable(tipo); 
            
            this.create(nuevaEntidad);
            return nuevaEntidad;
        });
    }

    /**
     * MÉTODO FALTANTE: Busca una entidad por su campo 'tipoContable'.
     *
     * @param tipo El enum a buscar.
     * @return un Optional con la entidad si se encuentra, o vacío si no.
     */
    private Optional<MovimientoContableEntity> findByTipo(TipoEnumsCosteo tipo) {
        // con el nombre del atributo en tu clase MovimientoContableEntity.
        String jpql = "SELECT m FROM MovimientoContableEntity m WHERE m.tipoContable = :tipo";
        
        TypedQuery<MovimientoContableEntity> query = entityManager.createQuery(jpql, MovimientoContableEntity.class);
        query.setParameter("tipo", tipo);

        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
}