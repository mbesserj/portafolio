package com.etl.interfaz; // O el paquete que prefieras, ej. com.etl.loader

import jakarta.persistence.EntityManager;

/**
 * Clase base abstracta para los procesadores de datos.
 * Reemplaza a CargaProcessorInterfaz y CargaProcessor para simplificar la jerarquía.
 * Define el contrato que todas las implementaciones (como BanChileProcessor) deben seguir.
 */
public abstract class AbstractCargaProcessor<T> {

    protected final EntityManager entityManager;

    public AbstractCargaProcessor(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Lógica específica para procesar un único objeto DTO.
     * Este método debe ser implementado por las subclases (ej. BanChileProcessor).
     * @param dto El objeto DTO a procesar (ej. guardar en la BD).
     */
    public abstract void procesar(T dto);
}