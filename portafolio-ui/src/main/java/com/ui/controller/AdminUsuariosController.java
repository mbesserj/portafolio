package com.ui.controller;

import com.model.entities.PerfilEntity;
import com.model.entities.UsuarioEntity;
import com.serv.service.PerfilService;
import com.serv.service.UsuarioService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class AdminUsuariosController {

    @FXML private ComboBox<PerfilEntity> cmbPerfiles;
    @FXML private TableView<UsuarioEntity> tablaUsuarios;
    @FXML private TableColumn<UsuarioEntity, String> colUsuario;
    @FXML private TableColumn<UsuarioEntity, String> colEmail;

    private final PerfilService perfilService;
    private final UsuarioService usuarioService;
    
    public AdminUsuariosController(UsuarioService usuarioService, PerfilService perfilService) {
        this.perfilService = perfilService;
        this.usuarioService = usuarioService;
    }

    @FXML
    public void initialize() {
        colUsuario.setCellValueFactory(new PropertyValueFactory<>("usuario"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("correo"));
        refrescarComboBoxPerfiles(null); // Carga inicial de perfiles
        
        Callback<ListView<PerfilEntity>, ListCell<PerfilEntity>> factory = lv -> new ListCell<>() {
            @Override
            protected void updateItem(PerfilEntity perfil, boolean empty) {
                super.updateItem(perfil, empty);
                setText(empty || perfil == null ? "" : perfil.getPerfil());
            }
        };
        cmbPerfiles.setCellFactory(factory);
        cmbPerfiles.setButtonCell(factory.call(null));
        
        cmbPerfiles.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                refrescarTablaUsuarios(newVal);
            }
        });
    }

    /**
     * Maneja la acción de crear un nuevo perfil.
     */
    @FXML
    void handleCrearPerfil(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Crear Nuevo Perfil");
        dialog.setHeaderText("Ingrese el nombre para el nuevo perfil (ej: CONSULTOR).");
        dialog.setContentText("Nombre:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(nombre -> {
            if (nombre.trim().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "El nombre del perfil no puede estar vacío.").show();
                return;
            }
            try {
                // El servicio se encarga de crear el perfil si no existe
                PerfilEntity nuevoPerfil = perfilService.buscarOCrearPorNombre(nombre.trim().toUpperCase());
                
                // Refresca el ComboBox y selecciona el nuevo perfil
                refrescarComboBoxPerfiles(nuevoPerfil);
                
                new Alert(Alert.AlertType.INFORMATION, "Perfil '" + nuevoPerfil.getPerfil() + "' creado con éxito.").show();
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, "Error al crear el perfil: " + e.getMessage()).show();
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Método auxiliar para recargar los perfiles en el ComboBox.
     * @param perfilASeleccionar El perfil que se debe seleccionar después de recargar.
     */
    private void refrescarComboBoxPerfiles(PerfilEntity perfilASeleccionar) {
        List<PerfilEntity> perfiles = perfilService.obtenerTodos();
        cmbPerfiles.setItems(FXCollections.observableArrayList(perfiles));
        
        if (perfilASeleccionar != null) {
            cmbPerfiles.setValue(perfilASeleccionar);
        }
    }
    
    private void refrescarTablaUsuarios(PerfilEntity perfil) {
        tablaUsuarios.setItems(FXCollections.observableArrayList(usuarioService.obtenerUsuariosPorPerfil(perfil)));
    }

    @FXML
    void handleAgregarUsuario(ActionEvent event) throws IOException {
        PerfilEntity perfilSeleccionado = cmbPerfiles.getValue();
        if (perfilSeleccionado == null) {
            new Alert(Alert.AlertType.WARNING, "Por favor, seleccione un perfil primero.").show();
            return;
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CrearUsuarioView.fxml"));
        Parent root = loader.load();

        Stage stage = new Stage();
        stage.setTitle("Crear Nuevo Usuario");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(new Scene(root));

        CrearUsuarioController controller = loader.getController();
        stage.showAndWait();

        UsuarioEntity nuevoUsuario = controller.getNuevoUsuario();
        if (nuevoUsuario != null) {
            // Asigna el perfil seleccionado al nuevo usuario
            usuarioService.cambiarPerfilDeUsuario(nuevoUsuario.getId(), null, perfilSeleccionado);
            refrescarTablaUsuarios(perfilSeleccionado);
        }
    }

    @FXML
    void handleDesactivarUsuario(ActionEvent event) {
        UsuarioEntity usuarioSeleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (usuarioSeleccionado == null) {
            new Alert(Alert.AlertType.WARNING, "Por favor, seleccione un usuario de la tabla.").show();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Está seguro de que desea desactivar al usuario " + usuarioSeleccionado.getUsuario() + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                usuarioService.desactivarUsuario(usuarioSeleccionado.getId());
                refrescarTablaUsuarios(cmbPerfiles.getValue());
            }
        });
    }

    @FXML
    void handleCambiarPerfil(ActionEvent event) {
        UsuarioEntity usuarioSeleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();
        PerfilEntity perfilOrigen = cmbPerfiles.getValue();

        if (usuarioSeleccionado == null || perfilOrigen == null) {
            new Alert(Alert.AlertType.WARNING, "Por favor, seleccione un perfil y un usuario.").show();
            return;
        }

        List<PerfilEntity> todosLosPerfiles = perfilService.obtenerTodos();
        todosLosPerfiles.remove(perfilOrigen); // No mostrar el perfil actual como opción de destino

        ChoiceDialog<PerfilEntity> dialog = new ChoiceDialog<>(null, todosLosPerfiles);
        dialog.setTitle("Cambiar Perfil");
        dialog.setHeaderText("Mover usuario '" + usuarioSeleccionado.getUsuario() + "' a un nuevo perfil.");
        dialog.setContentText("Seleccione el perfil de destino:");

        Optional<PerfilEntity> result = dialog.showAndWait();
        result.ifPresent(perfilDestino -> {
            usuarioService.cambiarPerfilDeUsuario(usuarioSeleccionado.getId(), perfilOrigen, perfilDestino);
            refrescarTablaUsuarios(perfilOrigen); 
        });
    }
}