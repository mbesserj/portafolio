package com.ui.controller;

import com.model.dto.ResumenSaldoEmpresaDto;
import com.serv.service.ResumenSaldoEmpresaService;
import com.ui.util.MainPaneAware;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ResumenSaldosController implements MainPaneAware {

    private BorderPane mainPane;
    private final ResumenSaldoEmpresaService resumenService;

    @FXML
    private TableView<ResumenSaldoEmpresaDto> tablaResumen;
    @FXML
    private TableColumn<ResumenSaldoEmpresaDto, String> colEmpresa;
    @FXML
    private TableColumn<ResumenSaldoEmpresaDto, String> colCustodio;
    @FXML
    private TableColumn<ResumenSaldoEmpresaDto, String> colCuenta;
    @FXML
    private TableColumn<ResumenSaldoEmpresaDto, BigDecimal> colSaldoClp;
    @FXML
    private TableColumn<ResumenSaldoEmpresaDto, BigDecimal> colSaldoUsd;
    @FXML
    private TableColumn<ResumenSaldoEmpresaDto, BigDecimal> colPorcentaje;

    public ResumenSaldosController(ResumenSaldoEmpresaService resumenService) {
        this.resumenService = resumenService;
    }

    @Override
    public void setMainPane(BorderPane mainPane) {
        this.mainPane = mainPane;
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        setupRowFactory();
        loadData();
    }

    private void setupTableColumns() {
        colEmpresa.setCellValueFactory(new PropertyValueFactory<>("empresa"));
        colCustodio.setCellValueFactory(new PropertyValueFactory<>("custodio"));
        colCuenta.setCellValueFactory(new PropertyValueFactory<>("cuenta"));
        colSaldoClp.setCellValueFactory(new PropertyValueFactory<>("saldoClp"));
        colSaldoUsd.setCellValueFactory(new PropertyValueFactory<>("saldoUsd"));
        colPorcentaje.setCellValueFactory(new PropertyValueFactory<>("porcentaje"));

        formatCurrencyColumn(colSaldoClp);
        formatCurrencyColumn(colSaldoUsd);
        formatPercentageColumn(colPorcentaje);
    }

    private void loadData() {
        Task<List<ResumenSaldoEmpresaDto>> task = new Task<>() {
            @Override
            protected List<ResumenSaldoEmpresaDto> call() throws Exception {
                return resumenService.obtenerResumenSaldos();
            }
        };

        task.setOnSucceeded(event -> tablaResumen.getItems().setAll(task.getValue()));
        task.setOnFailed(event -> task.getException().printStackTrace());

        new Thread(task).start();
    }

    @FXML
    private void handleCerrar() {
        if (mainPane != null) {
            mainPane.setCenter(null);
        }
    }

    // --- Métodos de Formato ---
    /**
     * Añade una fábrica de filas para aplicar estilos CSS. Lee la clase de
     * estilo directamente del DTO, desacoplando la vista de la lógica de
     * negocio. Esta es la versión simplificada y corregida.
     */
    private void setupRowFactory() {
        tablaResumen.setRowFactory(tv -> new TableRow<ResumenSaldoEmpresaDto>() {
            @Override
            protected void updateItem(ResumenSaldoEmpresaDto item, boolean empty) {
                super.updateItem(item, empty);

                // Siempre limpiamos cualquier estilo previo para evitar errores de renderizado
                getStyleClass().remove("bold-row");
                
                // Si el item es válido y tiene una clase de estilo, la aplicamos.
                if (item != null && !empty && item.getStyleClass() != null) {
                    getStyleClass().add("bold-row");
                }
            }
        });
    }

    private void formatCurrencyColumn(TableColumn<ResumenSaldoEmpresaDto, BigDecimal> column) {
        final Locale localeChile = new Locale("es", "CL");
        final NumberFormat integerFormat = NumberFormat.getNumberInstance(localeChile);
        integerFormat.setMaximumFractionDigits(0);

        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(integerFormat.format(item));
                    setStyle("-fx-alignment: CENTER-RIGHT;");
                }
            }
        });
    }

    private void formatPercentageColumn(TableColumn<ResumenSaldoEmpresaDto, BigDecimal> column) {
        final Locale localeChile = new Locale("es", "CL");
        final NumberFormat decimalFormat = NumberFormat.getNumberInstance(localeChile);
        decimalFormat.setMinimumFractionDigits(2);
        decimalFormat.setMaximumFractionDigits(2);

        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(decimalFormat.format(item) + " %");
                    setStyle("-fx-alignment: CENTER-RIGHT;");
                }
            }
        });
    }
}