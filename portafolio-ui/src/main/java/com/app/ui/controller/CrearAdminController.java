
package com.app.ui.controller;

import com.app.service.UsuarioService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class CrearAdminController {

    @FXML
    private TextField txtUsuario;
    @FXML
    private TextField txtEmail;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private PasswordField txtConfirmarPassword;
    @FXML
    private Button btnCrear;
    @FXML
    private Label lblMensaje;

    private final UsuarioService usuarioService;
    private boolean adminCreado = false;
    
    public CrearAdminController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @FXML
    void handleCrearAdmin(ActionEvent event) {
        String usuario = txtUsuario.getText();
        String email = txtEmail.getText();
        String password = txtPassword.getText();
        String confirmar = txtConfirmarPassword.getText();

        // Validaciones
        if (usuario.isEmpty() || email.isEmpty() || password.isEmpty()) {
            lblMensaje.setText("Todos los campos son obligatorios.");
            return;
        }
        if (!password.equals(confirmar)) {
            lblMensaje.setText("Las contraseñas no coinciden.");
            return;
        }

        try {
            // Llamamos al servicio para registrar el usuario
            usuarioService.registrarNuevoUsuario(usuario, password, email);
            adminCreado = true;
            
            // Cerramos la ventana
            Stage stage = (Stage) btnCrear.getScene().getWindow();
            stage.close();

        } catch (Exception e) {
            lblMensaje.setText("Error al crear usuario: " + e.getMessage());
            e.printStackTrace();
            adminCreado = false;
        }
    }

    public boolean isAdminCreado() {
        return adminCreado;
    }
}