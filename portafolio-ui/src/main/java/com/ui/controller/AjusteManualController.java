package com.ui.controller;

import com.model.dto.AjustePropuestoDto;
import com.model.entities.TransaccionEntity;
import com.model.enums.TipoAjuste;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Este controlador lo usa OperacionesTrxsControles para realizar ajustes manuales.
 */
public class AjusteManualController {

    // --- Componentes FXML de la Vista ---
    @FXML private Label lblTituloVentana;
    @FXML private Label lblTituloAjuste;
    @FXML private Label lblTransaccionId;
    @FXML private Label lblInstrumento;
    @FXML private Label lblFecha;
    @FXML private Label lblCantidadOriginal;
    @FXML private Button btnAprobar;
    @FXML private Button btnCancelar;
    @FXML private TextField txtCantidadAjuste;
    @FXML private Label lblPrecioPromedio;
    @FXML private TextField txtPrecioManual;
    @FXML private Label lblMontoTotal;
    @FXML private Label lblSaldoAnterior;

    // --- Variables de Estado ---
    private boolean aprobado = false;
    private BigDecimal precioPromedioCalculado = BigDecimal.ZERO;

    /**
     * Inicializa la ventana con los datos de referencia y la propuesta de ajuste.
     * Este es el punto de entrada principal para el controlador.
     *
     * @param transaccion La transacción original que requiere el ajuste.
     * @param tipo El tipo de ajuste (Ingreso o Egreso).
     * @param propuesta El DTO con los valores calculados (cantidad y precio) a proponer.
     */
    public void initData(TransaccionEntity transaccion, TipoAjuste tipo, AjustePropuestoDto propuesta) {
        // 1. Muestra datos informativos de la transacción original
        lblTransaccionId.setText(String.valueOf(transaccion.getId()));
        lblInstrumento.setText(transaccion.getInstrumento().getInstrumentoNemo());
        lblFecha.setText(transaccion.getFecha().toString());
        lblCantidadOriginal.setText(transaccion.getCantidad().toPlainString());

        // 2. Rellena los campos con los valores propuestos por el servicio
        if (propuesta != null) {
            txtCantidadAjuste.setText(propuesta.getCantidad().toPlainString());
            this.precioPromedioCalculado = propuesta.getPrecio();
            lblPrecioPromedio.setText(String.format("%,.2f", this.precioPromedioCalculado));
        }

        // 3. Configura listeners para que el total se actualice en tiempo real
        recalcularTotal(); // Llamada inicial para mostrar el total al abrir la ventana
        txtCantidadAjuste.textProperty().addListener((obs, oldVal, newVal) -> recalcularTotal());
        txtPrecioManual.textProperty().addListener((obs, oldVal, newVal) -> recalcularTotal());
        
        // 4. Configura textos y estilos de la ventana según el tipo de ajuste
        if (tipo == TipoAjuste.INGRESO) {
            lblTituloVentana.setText("Confirmar Ajuste por Ingreso");
            lblTituloAjuste.setText("Se creará un ajuste de INGRESO para saldar el déficit.");
            btnAprobar.setText("Confirmar y Crear Ajuste");
            btnAprobar.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        } else {
            lblTituloVentana.setText("Confirmar Ajuste por Egreso");
            lblTituloAjuste.setText("Se creará un ajuste de EGRESO basado en esta transacción.");
            btnAprobar.setText("Confirmar y Crear Ajuste");
            btnAprobar.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        }
        
        if (propuesta.getSaldoAnteriorFecha() != null && propuesta.getSaldoAnteriorCantidad() != null) {
            String textoSaldo = String.format("%,.2f al %s", 
                propuesta.getSaldoAnteriorCantidad(),
                propuesta.getSaldoAnteriorFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            );
            lblSaldoAnterior.setText(textoSaldo);
        }
    }

    /**
     * Calcula el monto total basándose en la cantidad y el precio (manual o promedio).
     * Se activa cada vez que el usuario modifica los campos de texto.
     */
    private void recalcularTotal() {
        try {
            BigDecimal cantidad = new BigDecimal(txtCantidadAjuste.getText().trim());
            BigDecimal precioFinal = getPrecioFinalInternal();
            BigDecimal total = cantidad.multiply(precioFinal);
            lblMontoTotal.setText(String.format("%,.2f", total));
        } catch (NumberFormatException e) {
            lblMontoTotal.setText("Valor inválido");
        }
    }
    
    /**
     * Lógica interna para determinar qué precio usar (el manual tiene prioridad).
     */
    private BigDecimal getPrecioFinalInternal() {
        String precioManualStr = txtPrecioManual.getText();
        if (precioManualStr != null && !precioManualStr.isBlank()) {
            try {
                return new BigDecimal(precioManualStr.trim());
            } catch (NumberFormatException e) {
                // Si el usuario escribe algo inválido, se asume 0 para el cálculo del total.
                return BigDecimal.ZERO;
            }
        }
        // Si el campo manual está vacío, se usa el precio promedio calculado.
        return this.precioPromedioCalculado;
    }

    // --- MÉTODOS PÚBLICOS para que el controlador principal recupere los datos ---

    /**
     * Devuelve la cantidad final ingresada por el usuario.
     */
    public BigDecimal getCantidadFinal() {
        try {
            return new BigDecimal(txtCantidadAjuste.getText().trim());
        } catch (Exception e) {
            // Devuelve cero si el valor es inválido para evitar errores.
            return BigDecimal.ZERO;
        }
    }

    /**
     * Devuelve el precio final (manual si existe; si no, el promedio).
     */
    public BigDecimal getPrecioFinal() {
        return getPrecioFinalInternal();
    }

    // --- Manejadores de Eventos y Lógica de la Ventana ---

    @FXML
    private void handleAprobar() {
        this.aprobado = true;
        closeWindow();
    }

    @FXML
    private void handleCancelar() {
        this.aprobado = false;
        closeWindow();
    }

    public boolean isAprobado() {
        return aprobado;
    }

    private void closeWindow() {
        Stage stage = (Stage) btnAprobar.getScene().getWindow();
        stage.close();
    }
}