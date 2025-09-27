/**
 * Módulo que define la capa de Interfaz de Usuario (UI) de la aplicación.
 */
module com.app.portafolio.ui {

    // --- DEPENDENCIAS DE MÓDULOS DEL PROYECTO ---

    // 1. Requiere el módulo de servicio para acceder a la lógica de negocio.
    // A través de este, obtiene acceso transitivo a los DTOs del modelo si es necesario.
    requires com.app.portafolio.service;
    requires com.app.portafolio.etl;
    requires com.app.portafolio.costing;

    // --- DEPENDENCIAS DE JAVAFX ---
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires static lombok;
    requires spring.security.crypto;

    // --- OTRAS DEPENDENCIAS ---
    requires org.slf4j;
    requires ch.qos.logback.classic;

    // --- PAQUETES QUE ESTE MÓDULO OFRECE A JAVAFX ---
    // Abre los paquetes de controladores y la clase principal al framework JavaFX
    // para que pueda usar reflexión para instanciar controladores y lanzar la app.
    opens com.app.ui.App to javafx.graphics, javafx.fxml;
    opens com.app.ui.controller to javafx.fxml;
    opens com.app.ui.util to javafx.fxml;

}