package com.ui.controller;

import com.model.dto.KardexReporteDto;
import com.ui.factory.AppFacade;
import com.ui.factory.BaseController;
import com.ui.util.MainPaneAware;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

import static com.ui.util.FormatUtils.createNumericCellFactory;

public class KardexController extends BaseController implements MainPaneAware {

    private BorderPane mainPane;
    private NavigatorService navigatorService;

    // --- Componentes FXML ---
    @FXML private FiltroGrupo filtroGrupo;
    @FXML private Button btnBuscar;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private TableView<KardexReporteDto> tablaKardex;
    @FXML private TableColumn<KardexReporteDto, LocalDate> colFecha;
    @FXML private TableColumn<KardexReporteDto, String> colTipo;
    @FXML private TableColumn<KardexReporteDto, String> colNemo;
    @FXML private TableColumn<KardexReporteDto, BigDecimal> colCantidadCompra;
    @FXML private TableColumn<KardexReporteDto, BigDecimal> colPrecioCompra;
    @FXML private TableColumn<KardexReporteDto, BigDecimal> colMontoCompra;
    @FXML private TableColumn<KardexReporteDto, BigDecimal> colCantidadVenta;
    @FXML private TableColumn<KardexReporteDto, BigDecimal> colPrecioVenta;
    @FXML private TableColumn<KardexReporteDto, LocalDate> colFechaConsumo;
    @FXML private TableColumn<KardexReporteDto, BigDecimal> colCostoFifo;
    @FXML private TableColumn<KardexReporteDto, BigDecimal> colCostoTotalFifo;
    @FXML private TableColumn<KardexReporteDto, BigDecimal> colMargen;
    @FXML private TableColumn<KardexReporteDto, BigDecimal> colUtilidadPerdida;
    @FXML private TableColumn<KardexReporteDto, BigDecimal> colSaldo;
    @FXML private TableColumn<KardexReporteDto, BigDecimal> colMonto;

    /**
     * Constructor que sigue el patrón de Inyección de Dependencias.
     * @param facade La fachada de la aplicación.
     * @param bundle El paquete de recursos para internacionalización.
     */
    public KardexController(AppFacade facade, ResourceBundle bundle) {
        super(facade, bundle);
    }

    /**
     * Método para que un controlador superior (como AppController) inyecte
     * el servicio de navegación.
     */
    public void setNavigatorService(NavigatorService navigatorService) {
        this.navigatorService = navigatorService;
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        setupFiltroListeners();
    }

    /**
     * Configura los listeners para los filtros en cascada.
     */
    private void setupFiltroListeners() {
        facade.obtenerEmpresasConTransacciones()
              .ifSuccess(filtroGrupo::setEmpresas)
              .ifError(errMsg -> showError("Error de Carga", errMsg));

        filtroGrupo.empresaProperty().addListener((obs, old, newVal) -> {
            filtroGrupo.limpiarCustodios();
            if (newVal != null) {
                facade.obtenerCustodiosConTransacciones(newVal.getId())
                      .ifSuccess(filtroGrupo::setCustodios)
                      .ifError(errMsg -> showError("Error de Carga", errMsg));
            }
        });

        filtroGrupo.custodioProperty().addListener((obs, old, newVal) -> {
            filtroGrupo.limpiarCuentas();
            if (newVal != null) {
                facade.obtenerCuentasConTransacciones(filtroGrupo.getEmpresaId(), newVal.getId())
                      .ifSuccess(filtroGrupo::setCuentas)
                      .ifError(errMsg -> showError("Error de Carga", errMsg));
            }
        });

        filtroGrupo.cuentaProperty().addListener((obs, old, newVal) -> {
            filtroGrupo.limpiarInstrumentos();
            if (newVal != null && !newVal.isBlank()) {
                facade.obtenerInstrumentosConTransacciones(filtroGrupo.getEmpresaId(), filtroGrupo.getCustodioId(), newVal)
                      .ifSuccess(filtroGrupo::setInstrumentos)
                      .ifError(errMsg -> showError("Error de Carga", errMsg));
            }
        });

        filtroGrupo.nemoValueProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                handleBuscar();
            } else {
                tablaKardex.getItems().clear();
            }
        });
    }

    @FXML
    void handleBuscar() {
        final Long empresaId = filtroGrupo.getEmpresaId();
        final Long custodioId = filtroGrupo.getCustodioId();
        final String cuentaSeleccionada = filtroGrupo.getCuenta();
        final Long instrumentoId = filtroGrupo.getInstrumentoId();

        if (instrumentoId == null) { return; }

        Task<List<KardexReporteDto>> buscarTask = new Task<>() {
            @Override
            protected List<KardexReporteDto> call() throws Exception {
                return facade.obtenerMovimientosKardex(empresaId, custodioId, cuentaSeleccionada, instrumentoId)
                             .getData();
            }
        };

        progressIndicator.visibleProperty().bind(buscarTask.runningProperty());
        tablaKardex.getItems().clear();

        buscarTask.setOnSucceeded(e -> {
            tablaKardex.setItems(FXCollections.observableArrayList(buscarTask.getValue()));
        });

        buscarTask.setOnFailed(e -> {
            showError("Error al Consultar", "Ocurrió un error al consultar el Kardex.", buscarTask.getException());
        });

        new Thread(buscarTask).start();
    }

    @FXML
    private void handleTableDoubleClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            KardexReporteDto dtoSeleccionado = tablaKardex.getSelectionModel().getSelectedItem();
            if (dtoSeleccionado != null && navigatorService != null) {
                navigatorService.mostrarVentanaDetallesKardex(dtoSeleccionado);
            }
        }
    }
    
    @Override
    public void setMainPane(BorderPane mainPane) { this.mainPane = mainPane; }

    @FXML
    private void handleCerrar(ActionEvent event) {
        if (mainPane != null) { mainPane.setCenter(null); }
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
}