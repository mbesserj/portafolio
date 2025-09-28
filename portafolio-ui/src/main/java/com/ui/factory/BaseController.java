package com.ui.factory;

import com.ui.util.Alertas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ResourceBundle;

public abstract class BaseController {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final AppFacade facade; // DEBE SER AppFacade
    protected final ResourceBundle bundle;

    protected BaseController(AppFacade facade, ResourceBundle bundle) {
        this.facade = facade;
        this.bundle = bundle;
    }

    // Métodos de ayuda que puedes tener aquí
    protected void showError(String title, String message, Throwable... e) {
        logger.error("{}: {} - Exception: {}", title, message, e.length > 0 ? e[0] : "N/A");
        Alertas.mostrarAlertaError(title, message);
    }

    protected void showSuccess(String message) {
        Alertas.mostrarAlertaExito("Éxito", message);
    }
}