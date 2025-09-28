package com.ui.controller;

import com.ui.factory.AppFacade;
import com.ui.factory.BaseController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.util.ResourceBundle;

public class CrearAdminController extends BaseController {
    @FXML private TextField usuarioField;
    @FXML private PasswordField contrasenaField;
    @FXML private Button crearButton;
    private boolean creadoExitosamente = false;

    public CrearAdminController(AppFacade facade, ResourceBundle bundle) {
        super(facade, bundle);
    }

    @FXML
    private void handleCrearAdmin(ActionEvent event) {
        String usuario = usuarioField.getText();
        String contrasena = contrasenaField.getText();
        
        facade.crearUsuarioAdmin(usuario, contrasena) // Este método debe existir en tu AppFacade
            .ifSuccess(usuarioDto -> {
                showSuccess(bundle.getString("admin.crear.exito"));
                this.creadoExitosamente = true;
                cerrarVentana();
            })
            .ifError(errorMessage -> showError(bundle.getString("admin.crear.error.titulo"), errorMessage));
    }

    public boolean isCreadoExitosamente() { return creadoExitosamente; }
    private void cerrarVentana() { ((Stage) crearButton.getScene().getWindow()).close(); }
}