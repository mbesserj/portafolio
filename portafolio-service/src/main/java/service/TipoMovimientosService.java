package com.app.service;

import com.app.dao.MovimientoContableDao;
import com.app.dao.TipoMovimientoDao;
import com.app.dto.TipoMovimientoEstado;
import com.app.entities.MovimientoContableEntity;
import com.app.entities.TipoMovimientoEntity;
import com.app.enums.TipoEnumsCosteo;
import com.app.interfaces.AbstractRepository;
import com.app.interfaces.TipoMovimientoInterfaz;
import com.app.utiles.LibraryInitializer;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de negocio para operaciones relacionadas con Tipos de Movimiento.
 * Contiene la lógica para actualizar el estado contable.
 */
public class TipoMovimientosService extends AbstractRepository {

    private final TipoMovimientoInterfaz tipoMovimientoRepository;

    /**
     * Constructor para la inyección de dependencias.
     * Recibe el repositorio que necesita para realizar sus consultas.
     * @param tipoMovimientoRepository El repositorio para acceder a los datos de TipoMovimiento.
     */
    public TipoMovimientosService(TipoMovimientoInterfaz tipoMovimientoRepository) {
        super();
        this.tipoMovimientoRepository = tipoMovimientoRepository;
    }

    /**
     * Lógica de negocio: Actualiza el estado contable de un movimiento.
     * Este es un "Comando", una acción que modifica el estado del sistema.
     */
    public void actualizarEstadoContable(Long tipoMovimientoId, TipoEnumsCosteo nuevoEstadoEnum) {
        EntityManager em = null;
        try {
            em = LibraryInitializer.getEntityManager();
            em.getTransaction().begin();

            TipoMovimientoDao tipoMovimientoDao = new TipoMovimientoDao(em);
            MovimientoContableDao movimientoContableDao = new MovimientoContableDao(em);
            
            MovimientoContableEntity movimientoContable = movimientoContableDao.buscarOCrearPorTipo(nuevoEstadoEnum);
            TipoMovimientoEntity tipoMovimientoAActualizar = tipoMovimientoDao.findById(tipoMovimientoId);

            if (tipoMovimientoAActualizar == null) {
                throw new IllegalStateException("No se encontró el TipoMovimiento con ID: " + tipoMovimientoId);
            }

            tipoMovimientoAActualizar.setMovimientoContable(movimientoContable);
            tipoMovimientoDao.update(tipoMovimientoAActualizar);

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Falló la actualización del estado contable.", e);
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }
    
    // --- Métodos de consulta que ahora DELEGAN al repositorio ---

    public Optional<TipoMovimientoEntity> buscarPorNombre(String tipoMovimiento) {
        return tipoMovimientoRepository.buscarPorNombre(tipoMovimiento);
    }
    
    public List<TipoMovimientoEstado> obtenerMovimientosConCriteria() {
        return tipoMovimientoRepository.obtenerMovimientosConCriteria();
    }
    
    public List<TipoMovimientoEntity> obtenerTodosLosTipos() {
        return tipoMovimientoRepository.obtenerTodosLosTipos();
    }
}