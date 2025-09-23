package com.app.ui.util;

import javafx.scene.control.Alert;

/**
 * Clase de utilidad para mostrar diálogos de alerta comunes en JavaFX.
 * Esto evita tener que escribir el mismo código de configuración de Alerta
 * en múltiples controladores.
 */
public final class Alertas {

    /**
     * Constructor privado para prevenir que esta clase de utilidad sea instanciada.
     */
    private Alertas() {
    }

    /**
     * Muestra una alerta de tipo INFORMACIÓN.
     * Útil para notificar al usuario sobre el éxito de una operación.
     *
     * @param titulo  El título de la ventana de la alerta.
     * @param mensaje El mensaje principal a mostrar en el diálogo.
     */
    public static void mostrarAlertaInfo(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null); // No usamos cabecera para un look más limpio
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
    
    /**
     * Un alias para mostrarAlertaInfo, comúnmente usado para mensajes de éxito.
     */
    public static void mostrarAlertaExito(String titulo, String mensaje) {
        mostrarAlertaInfo(titulo, mensaje);
    }

    /**
     * Muestra una alerta de tipo ERROR.
     * Útil para notificar al usuario que una operación ha fallado.
     *
     * @param titulo  El título de la ventana de la alerta.
     * @param mensaje El mensaje principal a mostrar en el diálogo.
     */
    public static void mostrarAlertaError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
    
    /**
     * Muestra una alerta de tipo ADVERTENCIA.
     * Útil para advertir al usuario sobre una acción potencialmente no deseada.
     *
     * @param titulo  El título de la ventana de la alerta.
     * @param mensaje El mensaje principal a mostrar en el diálogo.
     */
    public static void mostrarAlertaAdvertencia(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}