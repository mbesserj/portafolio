package com.app.ui.controller;

import com.app.ui.util.Alertas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

/**
 * Clase base para todos los controladores de la aplicación.
 * Proporciona funcionalidades comunes como logging, manejo de errores,
 * y acceso a servicios y recursos.
 */
public abstract class BaseController {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final ServiceFactory serviceFactory;
    protected final ResourceBundle resourceBundle;
    
    protected BaseController(ServiceFactory serviceFactory, ResourceBundle resourceBundle) {
        this.serviceFactory = serviceFactory;
        this.resourceBundle = resourceBundle;
    }
    
    // Constructor para controladores que no necesitan ResourceBundle
    protected BaseController(ServiceFactory serviceFactory) {
        this(serviceFactory, null);
    }
    
    /**
     * Muestra un error al usuario y lo registra en los logs
     */
    protected void showError(String title, String message, Throwable throwable) {
        logger.error("{}: {}", title, message, throwable);
        Alertas.mostrarAlertaError(title, message + (throwable != null ? ": " + throwable.getMessage() : ""));
    }
    
    /**
     * Muestra un error al usuario y lo registra en los logs
     */
    protected void showError(String title, String message) {
        showError(title, message, null);
    }
    
    /**
     * Muestra un mensaje de éxito al usuario
     */
    protected void showSuccess(String message) {
        logger.info("Operación exitosa: {}", message);
        Alertas.mostrarAlertaExito("Éxito", message);
    }
    
    /**
     * Muestra una advertencia al usuario
     */
    protected void showWarning(String title, String message) {
        logger.warn("{}: {}", title, message);
        Alertas.mostrarAlertaAdvertencia(title, message);
    }
    
    /**
     * Muestra información al usuario
     */
    protected void showInfo(String title, String message) {
        logger.info("{}: {}", title, message);
        Alertas.mostrarAlertaInfo(title, message);
    }
    
    /**
     * Obtiene un texto internacionalizado
     */
    protected String getText(String key) {
        if (resourceBundle != null && resourceBundle.containsKey(key)) {
            return resourceBundle.getString(key);
        }
        return key; // Fallback al key si no se encuentra
    }
    
    /**
     * Obtiene un texto internacionalizado con parámetros
     */
    protected String getText(String key, Object... params) {
        String template = getText(key);
        return String.format(template, params);
    }
    
    /**
     * Método que los controladores pueden sobrescribir para lógica de inicialización
     */
    protected void onInitialize() {
        // Template method - los controladores pueden sobrescribir
    }
    
    /**
     * Método que los controladores pueden sobrescribir para lógica de limpieza
     */
    protected void onCleanup() {
        // Template method - los controladores pueden sobrescribir
    }
}