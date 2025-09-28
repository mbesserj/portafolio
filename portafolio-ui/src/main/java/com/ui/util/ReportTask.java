package com.ui.util;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.function.Supplier;

/**
 * Una tarea genérica para cargar datos de un servicio en una TableView en segundo plano.
 * @param <T> El tipo de objeto que se mostrará en la tabla.
 */
public class ReportTask<T> extends Task<List<T>> {

    private final TableView<T> tableView;
    private final ProgressIndicator progressIndicator;
    private final Button actionButton; // Puede ser null
    private final Supplier<List<T>> serviceCall;

    public ReportTask(TableView<T> tableView, ProgressIndicator progressIndicator, Button actionButton, Supplier<List<T>> serviceCall) {
        this.tableView = tableView;
        this.progressIndicator = progressIndicator;
        this.actionButton = actionButton;
        this.serviceCall = serviceCall;

        // Limpiar la tabla al iniciar
        this.tableView.getItems().clear();
        
        // CAMBIO: Solo enlazar las propiedades si los componentes no son nulos.
        if (this.progressIndicator != null) {
            this.progressIndicator.visibleProperty().bind(this.runningProperty());
        }
        if (this.actionButton != null) {
            this.actionButton.disableProperty().bind(this.runningProperty());
        }
    }

    @Override
    protected List<T> call() throws Exception {
        return serviceCall.get();
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        tableView.setItems(FXCollections.observableArrayList(getValue()));
    }

    @Override
    protected void failed() {
        super.failed();
        new Alert(Alert.AlertType.ERROR, "Error al cargar los datos: " + getException().getMessage()).show();
        getException().printStackTrace();
    }
}