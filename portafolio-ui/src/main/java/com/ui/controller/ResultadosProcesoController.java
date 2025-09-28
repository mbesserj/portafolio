package com.ui.controller;

import com.model.dto.ResultadoCargaDto;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ResultadosProcesoController {
    @FXML private Label lblArchivos;
    @FXML private Label lblTransacciones;
    @FXML private Label lblErrores;
    @FXML private Label lblDuracion;
    @FXML private Label lblDetalles;

    public void initData(ResultadoCargaDto resultado) {
        lblTransacciones.setText(String.valueOf(resultado.getTransaccionesCreadas()));
        lblErrores.setText(String.valueOf(resultado.getErroresEncontrados()));
        lblDuracion.setText(resultado.getDuracionSegundos() + " segundos");
        lblDetalles.setText(resultado.getMensaje());
    }

    @FXML
    private void handleAceptar() {
        ((Stage) lblArchivos.getScene().getWindow()).close();
    }
}