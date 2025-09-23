package com.app.ui.controller;

import com.app.entities.UsuarioEntity;
import com.app.service.UsuarioService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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
            // 1. Llamamos al servicio.
            this.nuevoUsuario = usuarioService.registrarNuevoUsuario(
                txtUsuario.getText(), 
                txtPassword.getText(), 
                txtEmail.getText()
            );

            // 3. Cerramos la ventana.
            Stage stage = (Stage) btnCrear.getScene().getWindow();
            stage.close();
            
        } catch (Exception e) {
            lblMensaje.setText(e.getMessage());
            this.nuevoUsuario = null; // Nos aseguramos de que no se devuelva nada en caso de error
        }
    }

    public UsuarioEntity getNuevoUsuario() {
        return nuevoUsuario;
    }
}