package com.ui.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;

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
     * Muestra un diálogo de confirmación modal (OK/Cancelar).
     *
     * @param titulo El título de la ventana del diálogo.
     * @param mensaje El mensaje principal que se muestra al usuario.
     * @return un Optional que contiene ButtonType.OK si el usuario presionó Aceptar,
     * o un Optional vacío en cualquier otro caso.
     */
    public static Optional<ButtonType> mostrarConfirmacion(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null); // No usamos cabecera para un look más limpio.
        alert.setContentText(mensaje);

        // Muestra el diálogo y espera la respuesta del usuario.
        // El .filter() es la clave: solo deja pasar la respuesta si es "OK".
        // Si el usuario presiona "Cancelar" o cierra la ventana, el resultado será un Optional vacío.
        return alert.showAndWait()
                    .filter(response -> response == ButtonType.OK);
    }

    /**
     * Muestra una alerta de tipo INFORMACIÓN.
     * Útil para notificar al usuario sobre el éxito de una operación.
     */
    public static void mostrarAlertaInfo(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
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
     */
    public static void mostrarAlertaAdvertencia(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}