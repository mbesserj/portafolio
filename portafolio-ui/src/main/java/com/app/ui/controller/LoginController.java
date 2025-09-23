package com.app.ui.controller;

import com.app.service.AuthenticationService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;
    @FXML private Label lblMensaje;

    private final AuthenticationService authService;
    private boolean loginExitoso = false;
    
    public LoginController(AuthenticationService authService) {
        this.authService = authService;
    }

    @FXML
    void handleLogin(ActionEvent event) {
        String usuario = txtUsuario.getText();
        String password = txtPassword.getText();

        if (authService.autenticar(usuario, password)) {
            loginExitoso = true;
            Stage stage = (Stage) btnLogin.getScene().getWindow();
            stage.close();
        } else {
            lblMensaje.setText("Usuario o contraseña incorrectos.");
            loginExitoso = false;
        }
    }
    
    public boolean isLoginExitoso() {
        return loginExitoso;
    }
}