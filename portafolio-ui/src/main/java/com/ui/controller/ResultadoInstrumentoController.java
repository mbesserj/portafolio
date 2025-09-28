package com.ui.controller;

import com.model.dto.ResultadoInstrumentoDto;
import com.ui.factory.AppFacade;
import com.ui.factory.BaseController;
import com.ui.factory.ServiceResult;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;

import java.math.BigDecimal;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class ResultadoInstrumentoController extends BaseController implements Initializable {

    // --- Dependencias ---
    private ResourceBundle resourceBundle;

    // --- Componentes FXML ---
    @FXML
    private FiltroGrupo filtroGrupo;
    @FXML
    private Button btnBuscar;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private TableView<ResultadoInstrumentoDto> tablaResultados;
    @FXML
    private TableColumn<ResultadoInstrumentoDto, LocalDate> colFecha;
    @FXML
    private TableColumn<ResultadoInstrumentoDto, String> colTipo;
    @FXML
    private TableColumn<ResultadoInstrumentoDto, BigDecimal> colCantCompra;
    @FXML
    private TableColumn<ResultadoInstrumentoDto, BigDecimal> colCantVenta;
    @FXML
    private TableColumn<ResultadoInstrumentoDto, BigDecimal> colSaldo;
    @FXML
    private TableColumn<ResultadoInstrumentoDto, BigDecimal> colCompras;
    @FXML
    private TableColumn<ResultadoInstrumentoDto, BigDecimal> colVentas;
    @FXML
    private TableColumn<ResultadoInstrumentoDto, BigDecimal> colCostoVenta;
    @FXML
    private TableColumn<ResultadoInstrumentoDto, BigDecimal> colDividendos;
    @FXML
    private TableColumn<ResultadoInstrumentoDto, BigDecimal> colGastos;
    @FXML
    private TableColumn<ResultadoInstrumentoDto, BigDecimal> colUtilidad;
    @FXML
    private Label lblUtilidadTotal;

    public ResultadoInstrumentoController(AppFacade facade, ResourceBundle bundle) {
        super(facade, bundle);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Inicializa el componente de filtro con los servicios necesarios
        filtroGrupo.initializeComponent(facade);
        setupTableColumns();
    }

    private void setupTableColumns() {
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipoMovimiento"));
        colCantCompra.setCellValueFactory(new PropertyValueFactory<>("cant_compra"));
        colCantVenta.setCellValueFactory(new PropertyValueFactory<>("cant_venta"));
        colSaldo.setCellValueFactory(new PropertyValueFactory<>("saldo"));
        colCompras.setCellValueFactory(new PropertyValueFactory<>("compras"));
        colVentas.setCellValueFactory(new PropertyValueFactory<>("ventas"));
        colCostoVenta.setCellValueFactory(new PropertyValueFactory<>("costoDeVenta"));
        colDividendos.setCellValueFactory(new PropertyValueFactory<>("dividendos"));
        colGastos.setCellValueFactory(new PropertyValueFactory<>("gastos"));
        colUtilidad.setCellValueFactory(new PropertyValueFactory<>("utilidadRealizada"));

        formatDecimalColumn(colCantCompra);
        formatDecimalColumn(colCantVenta);
        formatDecimalColumn(colSaldo);
        formatCurrencyColumn(colCompras);
        formatCurrencyColumn(colVentas);
        formatCurrencyColumn(colCostoVenta);
        formatCurrencyColumn(colDividendos);
        formatCurrencyColumn(colGastos);
        formatColoredCurrencyColumn(colUtilidad);
    }

    @FXML
    private void handleBuscar() {
        // --- VALIDACIÓN DE FILTROS (CORREGIDA) ---
        if (filtroGrupo.getEmpresaId() == null
                || filtroGrupo.getCustodioId() == null
                || filtroGrupo.getCuenta() == null
                || filtroGrupo.getInstrumentoId() == null) {

            Alert alert = new Alert(Alert.AlertType.WARNING, resourceBundle.getString("error.seleccion.incompleta"));
            alert.showAndWait();
            return;
        }

        progressIndicator.setVisible(true);
        btnBuscar.setDisable(true);

        Task<List<ResultadoInstrumentoDto>> task = new Task<>() {
            @Override
            protected List<ResultadoInstrumentoDto> call() throws Exception {
                Long empresaId = filtroGrupo.getEmpresaId();
                Long custodioId = filtroGrupo.getCustodioId();
                String cuenta = filtroGrupo.getCuenta();
                Long instrumentoId = filtroGrupo.getInstrumentoId();

                ServiceResult<List<ResultadoInstrumentoDto>> result
                        = facade.obtenerHistorialResultados(empresaId, custodioId, cuenta, instrumentoId);

                if (result.isSuccess()) {
                    return result.getData();
                } else {
                    throw new RuntimeException(result.getErrorMessage());
                }
            }
        };

        task.setOnSucceeded(event -> {
            tablaResultados.getItems().setAll(task.getValue());
            actualizarLabelUtilidadTotal(task.getValue());
            progressIndicator.setVisible(false);
            btnBuscar.setDisable(false);
        });

        task.setOnFailed(event -> {
            progressIndicator.setVisible(false);
            btnBuscar.setDisable(false);
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error al buscar los resultados: " + task.getException().getMessage());
            alert.showAndWait();
        });

        new Thread(task).start();
    }

    private void actualizarLabelUtilidadTotal(List<ResultadoInstrumentoDto> resultados) {
        resultados.stream()
                .filter(dto -> "TOTALES".equals(dto.getTipoMovimiento()))
                .findFirst()
                .ifPresent(totales -> {
                    BigDecimal utilidadTotal = totales.getUtilidadRealizada();
                    Platform.runLater(() -> {
                        DecimalFormat currencyFormat = new DecimalFormat("$#,##0.00");
                        lblUtilidadTotal.setText(currencyFormat.format(utilidadTotal));
                        lblUtilidadTotal.setTextFill(utilidadTotal.compareTo(BigDecimal.ZERO) >= 0 ? Color.GREEN : Color.RED);
                    });
                });
    }

    // --- MÉTODOS HELPER PARA FORMATEAR CELDAS ---
    private void formatDecimalColumn(TableColumn<ResultadoInstrumentoDto, BigDecimal> column) {
        DecimalFormat format = new DecimalFormat("#,##0.00");
        column.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : format.format(item));
            }
        });
    }

    private void formatCurrencyColumn(TableColumn<ResultadoInstrumentoDto, BigDecimal> column) {
        DecimalFormat format = new DecimalFormat("$#,##0.00");
        column.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : format.format(item));
            }
        });
    }

    private void formatColoredCurrencyColumn(TableColumn<ResultadoInstrumentoDto, BigDecimal> column) {
        DecimalFormat format = new DecimalFormat("$#,##0.00");
        column.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTextFill(Color.BLACK);
                } else {
                    setText(format.format(item));
                    setTextFill(item.compareTo(BigDecimal.ZERO) >= 0 ? Color.GREEN : Color.RED);
                }
            }
        });
    }

    @FXML
    private void handleCerrar() {
        // Lógica para cerrar la ventana, si es necesario
    }
}
