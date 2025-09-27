package com.app.ui.controller;

import com.app.ui.factory.AppFacade;
import com.app.ui.factory.BaseController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.util.ResourceBundle;

public class LoginController extends BaseController {
    @FXML private TextField usuarioField;
    @FXML private PasswordField contrasenaField;
    @FXML private Button loginButton;
    private boolean loginExitoso = false;

    public LoginController(AppFacade facade, ResourceBundle bundle) {
        super(facade, bundle);
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String usuario = usuarioField.getText();
        String contrasena = contrasenaField.getText();

        facade.autenticarUsuario(usuario, contrasena)
            .ifSuccess(token -> {
                this.loginExitoso = true;
                cerrarVentana();
            })
            .ifError(errorMessage -> showError(bundle.getString("login.error.titulo"), errorMessage));
    }

    public boolean isLoginExitoso() { return loginExitoso; }
    private void cerrarVentana() { ((Stage) loginButton.getScene().getWindow()).close(); }
}