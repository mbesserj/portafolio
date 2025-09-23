package com.app.dao;

import com.app.entities.MovimientoContableEntity;
import com.app.entities.TipoMovimientoEntity;
import com.app.enums.TipoEnumsCosteo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class TipoMovimientoDao extends AbstractJpaDao<TipoMovimientoEntity, Long> {

    private static final Logger logger = LoggerFactory.getLogger(TipoMovimientoDao.class);

    public TipoMovimientoDao(EntityManager entityManager) {
        super(entityManager, TipoMovimientoEntity.class);
    }

    /**
     * Busca un TipoMovimiento por nombre. Si no existe, lo crea
     * y le asigna el MovimientoContable correcto (INGRESO, EGRESO, etc.)
     * basándose en su nombre.
     */
    public TipoMovimientoEntity findOrCreateByTipoMovimiento(String tipoMovimientoNombre, String descripcion) {
        String nombreNormalizado = (tipoMovimientoNombre == null || tipoMovimientoNombre.trim().isEmpty()) ? "Sin tipo movimiento asignado" : tipoMovimientoNombre.trim();

        // Buscamos si ya existe
        Optional<TipoMovimientoEntity> existente = findByNombre(nombreNormalizado);
        if (existente.isPresent()) {
            return existente.get();
        }

        // Si no existe, lo creamos con la configuración contable correcta.
        logger.info("El tipo de movimiento '{}' no existe. Creando uno nuevo...", nombreNormalizado);
        TipoMovimientoEntity nuevoTipoMovimiento = new TipoMovimientoEntity();
        nuevoTipoMovimiento.setTipoMovimiento(nombreNormalizado);
        nuevoTipoMovimiento.setDescripcion(descripcion);

        // Asignamos el tipo contable correcto según el nombre del movimiento.
        MovimientoContableEntity movimientoContable;
        switch (nombreNormalizado) {
            case "SALDO INICIAL":
            case "AJUSTE INGRESO":
            case "AJUSTE CUADRATURA":
            case "AJUSTE_AUTO_TOLERANCIA":
                movimientoContable = findMovimientoContable(TipoEnumsCosteo.INGRESO);
                nuevoTipoMovimiento.setEsSaldoInicial(nombreNormalizado.equals("SALDO INICIAL") || nombreNormalizado.equals("AJUSTE CUADRATURA"));
                break;

            case "AJUSTE EGRESO":
                movimientoContable = findMovimientoContable(TipoEnumsCosteo.EGRESO);
                break;

            default:
                // Por seguridad, si es un tipo desconocido, lo asignamos a NO_COSTEAR.
                movimientoContable = findMovimientoContable(TipoEnumsCosteo.NO_COSTEAR);
                break;
        }

        if (movimientoContable == null) {
            throw new IllegalStateException("No se encontró la configuración de Movimiento Contable en la BD. Asegúrese de que existan los registros para INGRESO, EGRESO, etc.");
        }

        nuevoTipoMovimiento.setMovimientoContable(movimientoContable);
        create(nuevoTipoMovimiento); // Guardamos el nuevo tipo de movimiento
        return nuevoTipoMovimiento;
    }

    /**
     * Busca un TipoMovimiento por su nombre y devuelve un Optional.
     */
    public Optional<TipoMovimientoEntity> findByNombre(String tipoMovimiento) {
        try {
            TypedQuery<TipoMovimientoEntity> query = entityManager.createQuery(
                "SELECT t FROM TipoMovimientoEntity t WHERE t.tipoMovimiento = :tipo",
                TipoMovimientoEntity.class
            );
            query.setParameter("tipo", tipoMovimiento);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Helper para buscar una entidad MovimientoContable por su tipo enum.
     */
    private MovimientoContableEntity findMovimientoContable(TipoEnumsCosteo tipo) {
        try {
            return entityManager.createQuery("SELECT mc FROM MovimientoContableEntity mc WHERE mc.tipoContable = :tipo", MovimientoContableEntity.class)
                .setParameter("tipo", tipo)
                .getSingleResult();
        } catch (NoResultException e) {
            // Este es un error de configuración crítico si no se encuentra INGRESO, EGRESO, etc.
            logger.error("Error crítico: No se encontró un MovimientoContableEntity para el tipo '{}'. Revise los datos base.", tipo);
            throw new IllegalStateException("Falta configuración de MovimientoContable para: " + tipo);
        }
    }
}