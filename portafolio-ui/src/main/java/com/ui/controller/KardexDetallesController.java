package com.ui.controller;

import com.model.dto.KardexReporteDto;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;

public class KardexDetallesController {

    @FXML private Label lblFechaTran;
    @FXML private Label lblTipoOper;
    @FXML private Label lblNemo;
    @FXML private Label lblUtilidad;
    @FXML private Label lblSaldoCantidad;
    @FXML private Label lblSaldoValor;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new java.util.Locale("es", "CL"));

    public void initData(KardexReporteDto dto) {
        lblFechaTran.setText(dto.getFechaTran() != null ? dto.getFechaTran().format(dateFormatter) : "-");
        lblTipoOper.setText(dto.getTipoOper() != null ? dto.getTipoOper() : "-");
        lblNemo.setText(dto.getNemo() != null ? dto.getNemo() : "-");
        lblUtilidad.setText(dto.getUtilidad() != null ? currencyFormatter.format(dto.getUtilidad()) : "-");
        lblSaldoCantidad.setText(dto.getSaldoCantidad() != null ? String.format("%,.2f", dto.getSaldoCantidad()) : "-");
        lblSaldoValor.setText(dto.getSaldoValor() != null ? currencyFormatter.format(dto.getSaldoValor()) : "-");
    }

    @FXML
    private void handleCerrar() {
        ((Stage) lblNemo.getScene().getWindow()).close();
    }
}