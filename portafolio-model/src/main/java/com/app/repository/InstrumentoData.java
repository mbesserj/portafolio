package com.app.repository;

/**
 * Interfaz que define el contrato para cualquier DTO que contenga
 * información básica de un instrumento para ser usado en filtros.
 */
public interface InstrumentoData {
    Long getInstrumentoId();
    String getNemo();
    String getNombreInstrumento();
}