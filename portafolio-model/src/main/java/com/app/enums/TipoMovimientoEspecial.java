package com.app.enums;

import java.util.Arrays;

public enum TipoMovimientoEspecial {
    AJUSTE_INGRESO("AJUSTE INGRESO"),
    AJUSTE_EGRESO("AJUSTE EGRESO"),
    SALDO_INICIAL("SALDO INICIAL"),
    AJUSTE_CUADRATURA("AJUSTE CUADRATURA"),
    
    // Un valor por defecto para cualquier otro texto que no coincida
    OTRO("OTRO");

    private final String nombre;

    TipoMovimientoEspecial(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    /**
     * Convierte un String de la base de datos a su correspondiente tipo de enum.
     * @param texto El texto del tipo de movimiento.
     * @return El enum correspondiente, o OTRO si no hay coincidencia.
     */
    public static TipoMovimientoEspecial fromString(String texto) {
        return Arrays.stream(values())
                .filter(tipo -> tipo.nombre.equalsIgnoreCase(texto))
                .findFirst()
                .orElse(OTRO);
    }
}