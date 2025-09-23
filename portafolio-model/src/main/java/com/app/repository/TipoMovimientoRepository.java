package com.app.repository;

import com.app.dto.TipoMovimientoEstado;
import com.app.entities.TipoMovimientoEntity;
import java.util.List;
import java.util.Optional;

/**
 * Contrato (Interfaz) para las operaciones de consulta sobre Tipos de Movimiento.
 * Vive en el módulo 'model' porque es parte del núcleo del dominio.
 */
public interface TipoMovimientoRepository {

    /**
     * Obtiene una lista de movimientos con su estado contable asociado.
     * @return Una lista de DTOs para el reporte.
     */
    List<TipoMovimientoEstado> obtenerMovimientosConCriteria();

    /**
     * Busca un TipoMovimiento por su nombre único.
     * @param tipoMovimiento El nombre del tipo de movimiento (ej. "INGRESO").
     * @return Un Optional que contiene la entidad si se encuentra, o vacío si no.
     */
    Optional<TipoMovimientoEntity> buscarPorNombre(String tipoMovimiento);

    /**
     * Obtiene una lista de todas las entidades TipoMovimiento.
     * @return Una lista completa de todos los tipos de movimiento.
     */
    List<TipoMovimientoEntity> obtenerTodosLosTipos();
}

