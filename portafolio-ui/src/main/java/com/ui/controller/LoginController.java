package com.ui.controller;

import com.ui.factory.AppFacade;
import com.ui.factory.BaseController;
import java.util.MissingResourceException;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ResourceBundle;
import javafx.scene.control.Alert;

public class LoginController extends BaseController {

    @FXML
    private TextField txtUsuario;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private Button btnIngresar;
    @FXML
    private Button btnSalir;
    @FXML
    private Label lblMensaje;

    private Stage loginStage;
    private boolean loginExitoso = false;

    // Constructor requerido por BaseController
    public LoginController(AppFacade facade, ResourceBundle bundle) {
        super(facade, bundle);
    }

    // Método para inyectar el Stage desde NavigatorService
    public void setStage(Stage stage) {
        this.loginStage = stage;
    }

    // Método público para consultar el resultado del login
    public boolean isLoginExitoso() {
        return loginExitoso;
    }

    @FXML
    private void handleIngresar() {
        String usuario = txtUsuario.getText();
        String password = txtPassword.getText();

        // Validar campos vacíos
        if (usuario == null || usuario.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {

            mostrarError("login.error.campos.vacios");
            return;
        }

        try {
            // Usar el servicio de autenticación a través del facade
            var authResult = facade.autenticarUsuario(usuario.trim(), password);

            if (authResult.isSuccess()) {
                loginExitoso = true;
                cerrarVentana();
            } else {
                mostrarError("login.error.credenciales");
                loginExitoso = false;
            }

        } catch (Exception e) {
            logger.error("Error durante autenticación", e);
            mostrarError("login.error.conexion");
            loginExitoso = false;
        }
    }

    @FXML
    private void handleSalir() {
        loginExitoso = false;
        cerrarVentana();
    }

    private void cerrarVentana() {
        if (loginStage != null) {
            loginStage.close();
        }
    }

    private void mostrarError(String mensajeKey) {
        // Validación defensiva para evitar NullPointerException
        if (lblMensaje != null && bundle != null) {
            try {
                String mensaje = bundle.getString(mensajeKey);
                lblMensaje.setText(mensaje);
                lblMensaje.setTextFill(Color.RED);
            } catch (MissingResourceException e) {
                // Fallback si no existe la clave en el bundle
                lblMensaje.setText("Error de autenticación");
                lblMensaje.setTextFill(Color.RED);
                logger.warn("Clave de mensaje no encontrada: {}", mensajeKey);
            }
        } else {
            // Fallback si no hay componentes UI disponibles
            logger.error("No se puede mostrar mensaje de error: {} (componentes UI no disponibles)", mensajeKey);
            // Opcionalmente mostrar una alerta
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error de autenticación");
            alert.showAndWait();
        }
    }

    @FXML
    private void initialize() {
        // Configurar el botón por defecto al presionar Enter
        if (btnIngresar != null) {
            btnIngresar.setDefaultButton(true);
        }

        // Limpiar mensaje de error al escribir
        if (txtUsuario != null) {
            txtUsuario.textProperty().addListener((obs, oldText, newText) -> limpiarMensaje());
        }
        if (txtPassword != null) {
            txtPassword.textProperty().addListener((obs, oldText, newText) -> limpiarMensaje());
        }
    }

    private void limpiarMensaje() {
        if (lblMensaje != null) {
            lblMensaje.setText("");
        }
    }
}
