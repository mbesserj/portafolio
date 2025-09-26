package com.app.ui.controller;

import com.app.entities.UsuarioEntity;
import com.app.service.UsuarioService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class CrearUsuarioController {
    @FXML private TextField txtUsuario;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblMensaje;
    @FXML private Button btnCrear;

    private UsuarioService usuarioService;
    private UsuarioEntity nuevoUsuario = null;
    
    public CrearUsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @FXML
    void handleCrearUsuario(ActionEvent event) {
        try {
            // Llamamos al servicio y manejamos el resultado
            UsuarioService.UserRegistrationResult result = usuarioService.registrarNuevoUsuario(
                txtUsuario.getText(), 
                txtPassword.getText(), 
                txtEmail.getText()
            );

            if (result.isSuccess()) { 
                this.nuevoUsuario = result.getUser();
                Stage stage = (Stage) btnCrear.getScene().getWindow();
                stage.close();
            } else {
                lblMensaje.setText(result.getErrorMessage()); 
                this.nuevoUsuario = null;
            }
            
        } catch (Exception e) {
            lblMensaje.setText(e.getMessage());
            this.nuevoUsuario = null;
        }
    }

    public UsuarioEntity getNuevoUsuario() {
        return nuevoUsuario;
    }
}