package com.ui.controller;

import com.model.entities.TransaccionEntity;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;

public class TransaccionDetallesController {

    @FXML private Label lblId;
    @FXML private Label lblFecha;
    @FXML private Label lblFolio;
    @FXML private Label lblInstrumento;
    @FXML private Label lblTipoMovimiento;
    @FXML private Label lblTipoContable;
    @FXML private Label lblCantidad;
    @FXML private Label lblPrecio;
    @FXML private Label lblTotal;
    @FXML private Label lblCosteado;
    @FXML private Label lblParaRevision;
    
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final NumberFormat numberFormatter = NumberFormat.getNumberInstance();

    /**
     * Inicializa la ventana con los datos de la transacción seleccionada.
     * @param transaccion La entidad completa a mostrar.
     */
    public void initData(TransaccionEntity transaccion) {
        numberFormatter.setMinimumFractionDigits(2);
        numberFormatter.setMaximumFractionDigits(6);
        
        lblId.setText(String.valueOf(transaccion.getId()));
        lblFecha.setText(transaccion.getFecha() != null ? transaccion.getFecha().format(dateFormatter) : "-");
        lblFolio.setText(transaccion.getFolio() != null ? transaccion.getFolio() : "-");
        lblInstrumento.setText(transaccion.getInstrumento() != null ? transaccion.getInstrumento().getInstrumentoNemo() : "-");
        lblTipoMovimiento.setText(transaccion.getTipoMovimiento() != null ? transaccion.getTipoMovimiento().getTipoMovimiento() : "-");
        lblTipoContable.setText(transaccion.getTipoMovimiento() != null && transaccion.getTipoMovimiento().getMovimientoContable() != null 
                ? transaccion.getTipoMovimiento().getMovimientoContable().getTipoContable().name() : "-");

        lblCantidad.setText(formatBigDecimal(transaccion.getCantidad()));
        lblPrecio.setText(formatBigDecimal(transaccion.getPrecio()));
        lblTotal.setText(formatBigDecimal(transaccion.getTotal()));

        lblCosteado.setText(transaccion.isCosteado() ? "Sí" : "No");
        lblParaRevision.setText(transaccion.isParaRevision() ? "Sí" : "No");
    }

    private String formatBigDecimal(BigDecimal number) {
        return number != null ? numberFormatter.format(number) : "-";
    }

    @FXML
    private void handleCerrar() {
        Stage stage = (Stage) lblId.getScene().getWindow();
        stage.close();
    }
}