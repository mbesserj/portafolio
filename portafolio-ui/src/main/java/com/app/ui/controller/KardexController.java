package com.app.ui.controller;

import com.app.ui.util.MainPaneAware;
import com.app.dto.KardexReporteDto;
import com.app.ui.util.Alertas;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.app.ui.util.FormatUtils.createNumericCellFactory;
import com.app.repositorio.KardexRepositoryImpl;
import java.io.IOException;
import java.util.ResourceBundle;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class KardexController implements MainPaneAware {

    // --- Referencia al panel principal ---
    private BorderPane mainPane;
    private final ResourceBundle resourceBundle;

    // --- Componentes FXML ---
    @FXML
    private FiltroGrupo filtroGrupo;
    @FXML
    private Button btnBuscar;
    @FXML
    private ProgressIndicator progressIndicator;

    // --- Columnas de la tabla ---
    @FXML
    private TableView<KardexReporteDto> tablaKardex;
    @FXML
    private TableColumn<KardexReporteDto, LocalDate> colFecha;
    @FXML
    private TableColumn<KardexReporteDto, String> colTipo;
    @FXML
    private TableColumn<KardexReporteDto, String> colNemo;
    @FXML
    private TableColumn<KardexReporteDto, BigDecimal> colCantidadCompra;
    @FXML
    private TableColumn<KardexReporteDto, BigDecimal> colPrecioCompra;
    @FXML
    private TableColumn<KardexReporteDto, BigDecimal> colMontoCompra;
    @FXML
    private TableColumn<KardexReporteDto, BigDecimal> colCantidadVenta;
    @FXML
    private TableColumn<KardexReporteDto, BigDecimal> colPrecioVenta;
    @FXML
    private TableColumn<KardexReporteDto, LocalDate> colFechaConsumo;
    @FXML
    private TableColumn<KardexReporteDto, BigDecimal> colCostoFifo;
    @FXML
    private TableColumn<KardexReporteDto, BigDecimal> colCostoTotalFifo;
    @FXML
    private TableColumn<KardexReporteDto, BigDecimal> colMargen;
    @FXML
    private TableColumn<KardexReporteDto, BigDecimal> colUtilidadPerdida;
    @FXML
    private TableColumn<KardexReporteDto, BigDecimal> colSaldo;
    @FXML
    private TableColumn<KardexReporteDto, BigDecimal> colMonto;

    // --- Servicios del Core ---
    private final KardexRepositoryImpl kardexService;
    private final ServiceFactory serviceFactory;

    public KardexController(KardexRepositoryImpl kardexService, ServiceFactory serviceFactory, ResourceBundle resourceBundle) {
        this.kardexService = kardexService;
        this.serviceFactory = serviceFactory;
        this.resourceBundle = resourceBundle;
    }

    @FXML
    public void initialize() {
        setupTableColumns();

        filtroGrupo.initializeComponent(
                serviceFactory.getEmpresaService(),
                serviceFactory.getFiltroService()
        );

        // Este listener se dispara cada vez que el usuario cambia el instrumento seleccionado.
        filtroGrupo.nemoValueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) { 
                handleBuscar(); 
            } else {
                tablaKardex.getItems().clear(); 
            }
        });
    }

    /**
     * El AppController usará este método para pasar la referencia de su
     * BorderPane.
     */
    @Override
    public void setMainPane(BorderPane mainPane) {
        this.mainPane = mainPane;
    }

    @FXML
    private void handleCerrar(ActionEvent event) {
        if (mainPane != null) {
            mainPane.setCenter(null);
        }
    }

    private void setupTableColumns() {
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaTran"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipoOper"));
        colNemo.setCellValueFactory(new PropertyValueFactory<>("nemo"));
        colCantidadCompra.setCellValueFactory(new PropertyValueFactory<>("cantCompra"));
        colPrecioCompra.setCellValueFactory(new PropertyValueFactory<>("precioCompra"));
        colMontoCompra.setCellValueFactory(new PropertyValueFactory<>("montoCompra"));
        colCantidadVenta.setCellValueFactory(new PropertyValueFactory<>("cantUsada"));
        colPrecioVenta.setCellValueFactory(new PropertyValueFactory<>("precioVenta"));
        colFechaConsumo.setCellValueFactory(new PropertyValueFactory<>("fechaCompra"));
        colCostoFifo.setCellValueFactory(new PropertyValueFactory<>("costoFifo"));
        colCostoTotalFifo.setCellValueFactory(new PropertyValueFactory<>("costoOper"));
        colMargen.setCellValueFactory(new PropertyValueFactory<>("margen"));
        colUtilidadPerdida.setCellValueFactory(new PropertyValueFactory<>("utilidad"));
        colSaldo.setCellValueFactory(new PropertyValueFactory<>("saldoCantidad"));
        colMonto.setCellValueFactory(new PropertyValueFactory<>("saldoValor"));

        Callback<TableColumn<KardexReporteDto, BigDecimal>, TableCell<KardexReporteDto, BigDecimal>> numericCellFactory = createNumericCellFactory("#,##0.0");

        colCantidadCompra.setCellFactory(numericCellFactory);
        colPrecioCompra.setCellFactory(numericCellFactory);
        colMontoCompra.setCellFactory(numericCellFactory);
        colCantidadVenta.setCellFactory(numericCellFactory);
        colPrecioVenta.setCellFactory(numericCellFactory);
        colCostoFifo.setCellFactory(numericCellFactory);
        colCostoTotalFifo.setCellFactory(numericCellFactory);
        colMargen.setCellFactory(numericCellFactory);
        colUtilidadPerdida.setCellFactory(numericCellFactory);
        colSaldo.setCellFactory(numericCellFactory);
        colMonto.setCellFactory(numericCellFactory);
    }

    void handleBuscar() {
        final Long empresaId = filtroGrupo.getEmpresaId();
        final Long custodioId = filtroGrupo.getCustodioId();
        final String cuentaSeleccionada = filtroGrupo.getCuenta();
        final Long instrumentoId = filtroGrupo.getInstrumentoId();

        if (instrumentoId == null) {
            return;
        }

        Task<List<KardexReporteDto>> buscarTask = new Task<>() {
            @Override
            protected List<KardexReporteDto> call() throws Exception {
                return kardexService.obtenerMovimientosPorGrupo(empresaId, custodioId, cuentaSeleccionada, instrumentoId);
            }
        };

        progressIndicator.visibleProperty().bind(buscarTask.runningProperty());
        tablaKardex.getItems().clear();

        buscarTask.setOnSucceeded(e -> {
            tablaKardex.setItems(FXCollections.observableArrayList(buscarTask.getValue()));
        });

        buscarTask.setOnFailed(e -> {
            Throwable ex = buscarTask.getException();
            Alertas.mostrarAlertaError("Error al Consultar", "Ocurrió un error al consultar el Kardex: " + ex.getMessage());
            ex.printStackTrace();
        });

        new Thread(buscarTask).start();
    }
    
    @FXML
    private void handleTableDoubleClick(MouseEvent event) {
        if (event.getClickCount() == 2) { // Verificar doble clic
            KardexReporteDto dtoSeleccionado = tablaKardex.getSelectionModel().getSelectedItem();
            if (dtoSeleccionado != null) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/KardexDetallesView.fxml"), resourceBundle);
                    // No necesitas controller factory si el controlador de detalles tiene un constructor vacío
                    Parent view = loader.load();
                    
                    KardexDetallesController controller = loader.getController();
                    controller.initData(dtoSeleccionado); // Pasamos el DTO directamente

                    Stage stage = new Stage();
                    stage.setTitle("Detalles del Movimiento de Kárdex");
                    stage.initModality(Modality.APPLICATION_MODAL);
                    stage.initOwner(mainPane.getScene().getWindow());
                    stage.setScene(new Scene(view));
                    stage.showAndWait();

                } catch (IOException e) {
                    e.printStackTrace();
                    Alertas.mostrarAlertaError("Error de UI", "No se pudo abrir la ventana de detalles.");
                }
            }
        }
    }
}
