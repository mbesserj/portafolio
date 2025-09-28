package com.ui.util;

/**
 * Mantiene el estado contextual de la interfaz de usuario.
 * Por ejemplo, puede rastrear si una operación larga ya está en progreso.
 */
public class UIContext {
    private boolean operationInProgress = false;

    public boolean isOperationInProgress() {
        return operationInProgress;
    }

    public void setOperationInProgress(boolean operationInProgress) {
        this.operationInProgress = operationInProgress;
    }
}