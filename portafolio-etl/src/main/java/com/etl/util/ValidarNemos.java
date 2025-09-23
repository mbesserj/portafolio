
package com.etl.util;

public class ValidarNemos {


    public static String normalizarInstrumentoNemo(String nemo, String tipoClase) {
        if (nemo == null || nemo.trim().equals("--")) {
            if (tipoClase.equals("C") || tipoClase.equals("S") || tipoClase.equals("T")) {
                return "Movimiento Caja";
            }
        }
        return nemo;
    }
}