package com.app.dto;

import java.time.Duration;

public class ResultadoCargaDto {

    private final int transaccionesCreadas;
    private final int erroresEncontrados;
    private final Duration duracion;
    private final String mensaje;

    public ResultadoCargaDto(int trxs, int errores, Duration duracion, String mensaje) {
        this.transaccionesCreadas = trxs;
        this.erroresEncontrados = errores;
        this.duracion = duracion;
        this.mensaje = mensaje;
    }

    // --- Getters para todos los campos ---
    public int getTransaccionesCreadas() {
        return transaccionesCreadas;
    }

    public int getErroresEncontrados() {
        return erroresEncontrados;
    }

    public String getMensaje() {
        return mensaje;
    }

    public long getDuracionSegundos() {
        return duracion.toSeconds();
    }

    /**
     * Crea una instancia de resultado para operaciones exitosas.
     */
    public static ResultadoCargaDto exitoso(int registrosProcesados, Duration duracion, String mensaje) {
        return new ResultadoCargaDto(registrosProcesados, 0, duracion, mensaje);
    }

    /**
     * Crea una instancia de resultado para operaciones fallidas.
     */
    public static ResultadoCargaDto fallido(String mensajeDeError) {
        return new ResultadoCargaDto(0, 0, Duration.ZERO, mensajeDeError);
    }

    public int getRegistrosProcesados() {
        return this.transaccionesCreadas;
    }

    public Duration getDuracion() {
        return this.duracion;
    }
}
