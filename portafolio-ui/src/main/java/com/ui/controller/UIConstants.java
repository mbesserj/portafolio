package com.ui.controller;

/**
 * Constantes compartidas para la interfaz de usuario
 */
public final class UIConstants {
    
    private UIConstants() {
    }
    
    // --- Estilos CSS ---
    public static final class Styles {
        public static final String FILA_IGNORADA = "-fx-background-color: #e0e0e0; -fx-strikethrough: true;";
        public static final String FILA_REVISION = "-fx-background-color: #ffcccc;";
        public static final String FILA_NORMAL = "";
        public static final String BUTTON_SUCCESS = "button-exito";
        public static final String BUTTON_DANGER = "button-peligro";
        public static final String BUTTON_PRIMARY = "button-accion";
        public static final String TITULO_VISTA = "titulo-vista";
    }
    
    // --- Valores especiales ---
    public static final class Values {
        public static final String TODOS_INSTRUMENTOS = "(Todos)";
        public static final String TODOS_CUSTODIOS = "Todos los custodios";
        public static final String MANUAL_FOLIO = "MANUAL";
        public static final String TOTALES_ROW = "TOTALES";
        public static final Long TODOS_INSTRUMENTOS_ID = -1L;
        public static final Long TODOS_CUSTODIOS_ID = 0L;
    }
    
    // --- Formatos numéricos ---
    public static final class Formats {
        public static final String CURRENCY = "$#,##0.00";
        public static final String INTEGER = "#,##0";
        public static final String DECIMAL_2 = "#,##0.00";
        public static final String DECIMAL_4 = "#,##0.0000";
        public static final String PERCENTAGE = "#,##0.00 %";
        public static final String DATE = "dd/MM/yyyy";
    }
    
    // --- Claves de i18n comunes ---
    public static final class I18nKeys {
        public static final String ERROR_TITULO = "error.titulo";
        public static final String EXITO_TITULO = "exito.titulo";
        public static final String ADVERTENCIA_TITULO = "advertencia.titulo";
        public static final String CONFIRMACION_TITULO = "confirmacion.titulo";
        public static final String BUTTON_BUSCAR = "button.buscar";
        public static final String BUTTON_CERRAR = "button.cerrar";
        public static final String BUTTON_CANCELAR = "button.cancelar";
        public static final String BUTTON_GUARDAR = "button.guardar";
        public static final String BUTTON_ELIMINAR = "button.eliminar";
        public static final String LABEL_EMPRESA = "label.empresa";
    }
    
}