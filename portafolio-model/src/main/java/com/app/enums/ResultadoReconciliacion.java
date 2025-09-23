package com.app.enums; // o el paquete que corresponda

public class ResultadoReconciliacion {

    // Un enum anidado para los diferentes tipos de resultados
    public enum Estado {
        EXITO,             // El historial cuadró perfectamente
        EXITO_CON_AJUSTE,  // Se encontró una inconsistencia y se creó un ajuste
        FALLO              // El proceso no pudo completarse
    }

    public final Estado estado;
    public final String mensaje; // Un mensaje descriptivo para mostrar al usuario

    public ResultadoReconciliacion(Estado estado, String mensaje) {
        this.estado = estado;
        this.mensaje = mensaje;
    }
}