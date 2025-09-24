package com.app.interfaces;

/**
 * Interfaz que define el contrato para cualquier DTO que contenga
 * información básica de un instrumento para ser usado en filtros.
 */
public interface InstrumentoDataInterfaz {
    Long getInstrumentoId();
    String getNemo();
    String getNombreInstrumento();
}