package com.ui.controller;

import com.model.dto.ConfrontaSaldoDto;
import com.serv.service.ConfrontaService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public class ConfrontaSaldosController {

    private final ConfrontaService confrontaService;

    // --- Componentes FXML ---
    @FXML
    private Button btnBuscar;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private TableView<ConfrontaSaldoDto> tablaDiferencias;

    // --- Columnas de la tabla ---
    @FXML
    private TableColumn<ConfrontaSaldoDto, String> colEmpresa;
    @FXML
    private TableColumn<ConfrontaSaldoDto, String> colCustodio;
    @FXML
    private TableColumn<ConfrontaSaldoDto, String> colInstrumento;
    @FXML
    private TableColumn<ConfrontaSaldoDto, String> colCuenta;
    @FXML
    private TableColumn<ConfrontaSaldoDto, LocalDate> colFechaKardex;
    @FXML
    private TableColumn<ConfrontaSaldoDto, BigDecimal> colCantidadKardex;
    @FXML
    private TableColumn<ConfrontaSaldoDto, BigDecimal> colValorKardex;
    @FXML
    private TableColumn<ConfrontaSaldoDto, LocalDate> colFechaSaldos;
    @FXML
    private TableColumn<ConfrontaSaldoDto, BigDecimal> colCantidadSaldos;
    @FXML
    private TableColumn<ConfrontaSaldoDto, BigDecimal> colValorSaldos;
    @FXML
    private TableColumn<ConfrontaSaldoDto, BigDecimal> colDifCantidad;

    public ConfrontaSaldosController(ConfrontaService confrontaService) {
        this.confrontaService = confrontaService;
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        handleBuscarDiferencias();
    }

    private void setupTableColumns() {
        colEmpresa.setCellValueFactory(new PropertyValueFactory<>("empresaNombre"));
        colCustodio.setCellValueFactory(new PropertyValueFactory<>("custodioNombre"));
        colInstrumento.setCellValueFactory(new PropertyValueFactory<>("instrumentoNemo"));
        colCuenta.setCellValueFactory(new PropertyValueFactory<>("cuenta"));
        colFechaKardex.setCellValueFactory(new PropertyValueFactory<>("ultimaFechaKardex"));
        colCantidadKardex.setCellValueFactory(new PropertyValueFactory<>("cantidadKardex"));
        colValorKardex.setCellValueFactory(new PropertyValueFactory<>("valorKardex"));
        colFechaSaldos.setCellValueFactory(new PropertyValueFactory<>("ultimaFechaSaldos"));
        colCantidadSaldos.setCellValueFactory(new PropertyValueFactory<>("cantidadMercado"));
        colValorSaldos.setCellValueFactory(new PropertyValueFactory<>("valorMercado"));
        colDifCantidad.setCellValueFactory(new PropertyValueFactory<>("diferenciaCantidad"));

        formatDecimalColumn(colCantidadKardex, 4);
        formatCurrencyColumn(colValorKardex);
        formatDecimalColumn(colCantidadSaldos, 4);
        formatCurrencyColumn(colValorSaldos);
        formatDecimalColumn(colDifCantidad, 4);
    }

    @FXML
    private void handleBuscarDiferencias() {
        Task<List<ConfrontaSaldoDto>> task = new Task<>() {
            @Override
            protected List<ConfrontaSaldoDto> call() throws Exception {
                return confrontaService.obtenerDiferenciasDeSaldos();
            }
        };

        task.setOnRunning(e -> {
            progressIndicator.setVisible(true);
            btnBuscar.setDisable(true);
            tablaDiferencias.getItems().clear();
        });

        task.setOnSucceeded(e -> {
            progressIndicator.setVisible(false);
            btnBuscar.setDisable(false);
            tablaDiferencias.getItems().setAll(task.getValue());
        });

        task.setOnFailed(e -> {
            progressIndicator.setVisible(false);
            btnBuscar.setDisable(false);
            task.getException().printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error de Datos", "No se pudieron cargar las diferencias: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    // --- Métodos de ayuda ---
    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String contenido) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(contenido);
        alert.showAndWait();
    }

    private void formatCurrencyColumn(TableColumn<ConfrontaSaldoDto, BigDecimal> column) {
        final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CL"));
        currencyFormat.setMaximumFractionDigits(2);

        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : currencyFormat.format(item));
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });
    }

    private void formatDecimalColumn(TableColumn<ConfrontaSaldoDto, BigDecimal> column, int decimales) {
        final NumberFormat decimalFormat = NumberFormat.getNumberInstance(Locale.GERMANY);
        decimalFormat.setMinimumFractionDigits(decimales);
        decimalFormat.setMaximumFractionDigits(decimales);

        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : decimalFormat.format(item));
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });
    }
}
