package com.etl.util;

public class RutValidator {

    public static boolean esRutValido(String rut) {
        if (rut == null || rut.isEmpty()) {
            return false;
        }

        rut = rut.replace(".", "").replace("-", "").toUpperCase();

        // Separar cuerpo y dígito verificador
        int guionIndex = rut.length() - 1;
        String cuerpo = rut.substring(0, guionIndex);
        char dv = rut.charAt(guionIndex);

        try {
            int rutNum = Integer.parseInt(cuerpo);
            char dvEsperado = calcularDigitoVerificador(rutNum);
            return dv == dvEsperado;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String normalizarRut(String rut) {
        if (rut == null) {
            return null;
        }
        return rut.replace(".", "").replace("-", "").toUpperCase();
    }

    private static char calcularDigitoVerificador(int rut) {
        int suma = 0;
        int multiplicador = 2;

        while (rut > 0) {
            int digito = rut % 10;
            suma += digito * multiplicador;
            rut /= 10;
            multiplicador = (multiplicador == 7) ? 2 : multiplicador + 1;
        }

        int resto = suma % 11;
        int dv = 11 - resto;

        if (dv == 11) {
            return '0';
        }
        if (dv == 10) {
            return 'K';
        }
        return (char) ('0' + dv);
    }
}
