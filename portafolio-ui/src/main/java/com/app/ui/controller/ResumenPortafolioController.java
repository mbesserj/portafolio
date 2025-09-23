package com.app.ui.controller;

import com.app.dto.ResumenInstrumentoDto;
import com.app.service.ResumenPortafolioService;
import com.app.ui.util.MainPaneAware;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ResumenPortafolioController implements MainPaneAware {

    private BorderPane mainPane;
    private final ResumenPortafolioService resumenService;
    private final ServiceFactory serviceFactory;
    private final ResourceBundle resourceBundle;
    private final Locale localeChile = new Locale("es", "CL");

    private List<ResumenInstrumentoDto> listaCompletaResumen = new ArrayList<>();

    // --- Componentes FXML ---
    @FXML private FiltroPortafolio filtroGrupo;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private TableView<ResumenInstrumentoDto> tablaResumen;
    @FXML private Label lblTotalUtilidadRealizada;
    @FXML private Label lblTotalUtilidadNoRealizada;

    // --- Columnas ---
    @FXML private TableColumn<ResumenInstrumentoDto, String> colNemo;
    @FXML private TableColumn<ResumenInstrumentoDto, String> colInstrumento;
    @FXML private TableColumn<ResumenInstrumentoDto, BigDecimal> colSaldo;
    @FXML private TableColumn<ResumenInstrumentoDto, BigDecimal> colCosto;
    @FXML private TableColumn<ResumenInstrumentoDto, BigDecimal> colMercado;
    @FXML private TableColumn<ResumenInstrumentoDto, BigDecimal> colDividendos;
    @FXML private TableColumn<ResumenInstrumentoDto, BigDecimal> colGastos;
    @FXML private TableColumn<ResumenInstrumentoDto, BigDecimal> colUtilRealizada;
    @FXML private TableColumn<ResumenInstrumentoDto, BigDecimal> colUtilNoRealizada;
    @FXML private TableColumn<ResumenInstrumentoDto, BigDecimal> colRentabilidad;

    public ResumenPortafolioController(ResumenPortafolioService resumenService, ServiceFactory serviceFactory, ResourceBundle resourceBundle) {
        this.resumenService = resumenService;
        this.serviceFactory = serviceFactory;
        this.resourceBundle = resourceBundle;
    }

    @FXML
    public void initialize() {
        filtroGrupo.initializeComponent(serviceFactory.getEmpresaService(), serviceFactory.getFiltroService());
        setupTableColumns();
        
        // --- CAMBIO 1: CREAMOS LISTENERS INDIVIDUALES PARA CADA FILTRO PRINCIPAL ---
        // Se asume que tu clase FiltroPortafolio expone los ComboBox con getters como getCmbEmpresa(), etc.
        filtroGrupo.getCmbEmpresa().valueProperty().addListener((obs, oldVal, newVal) -> onFiltroPrincipalChanged());
        filtroGrupo.getCmbCustodio().valueProperty().addListener((obs, oldVal, newVal) -> onFiltroPrincipalChanged());
        filtroGrupo.getCmbCuenta().valueProperty().addListener((obs, oldVal, newVal) -> onFiltroPrincipalChanged());

        // El listener para el nemónico se mantiene igual, ya que solo filtra los datos ya cargados.
        filtroGrupo.nemoValueProperty().addListener((obs, oldVal, newVal) -> filtrarYActualizarTotales());

        // Lógica para estilizar la fila de TOTALES (se mantiene igual)
        tablaResumen.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(ResumenInstrumentoDto item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("fila-totales");
                if (item != null && "TOTALES".equals(item.getNemo())) {
                    getStyleClass().add("fila-totales");
                }
            }
        });
    }
    
    // --- CAMBIO 2: NUEVO MÉTODO PARA CENTRALIZAR LA LÓGICA DE CARGA ---
    /**
     * Se ejecuta cada vez que cambia un filtro principal (empresa, custodio, cuenta).
     * Verifica si la selección es válida y decide si cargar datos o limpiar la vista.
     */
    private void onFiltroPrincipalChanged() {
        // Se asume que FiltroPortafolio tiene un método que valida si los 3 campos principales están seleccionados.
        if (filtroGrupo.validSelectionSinInstrumentoProperty().get()) {
            cargarResumenCompleto();
        } else {
            limpiarVista();
        }
    }

    private void setupTableColumns() {
        // ... (este método se mantiene sin cambios)
        colNemo.setCellValueFactory(new PropertyValueFactory<>("nemo"));
        colInstrumento.setCellValueFactory(new PropertyValueFactory<>("nombreInstrumento"));
        colSaldo.setCellValueFactory(new PropertyValueFactory<>("saldoDisponible"));
        colCosto.setCellValueFactory(new PropertyValueFactory<>("costoFifo"));
        colMercado.setCellValueFactory(new PropertyValueFactory<>("valorDeMercado"));
        colDividendos.setCellValueFactory(new PropertyValueFactory<>("totalDividendos"));
        colGastos.setCellValueFactory(new PropertyValueFactory<>("totalGastos"));
        colUtilRealizada.setCellValueFactory(new PropertyValueFactory<>("utilidadRealizada"));
        colUtilNoRealizada.setCellValueFactory(new PropertyValueFactory<>("utilidadNoRealizada"));
        colRentabilidad.setCellValueFactory(new PropertyValueFactory<>("rentabilidad"));
        
        String formatoEntero = "#,##0";
        NumberFormat formatoMoneda = new DecimalFormat(formatoEntero, new DecimalFormatSymbols(localeChile));
        colSaldo.setCellFactory(column -> createRightAlignedNumericCell("#,##0.00"));
        colCosto.setCellFactory(column -> createRightAlignedNumericCell(formatoEntero));
        colMercado.setCellFactory(column -> createRightAlignedNumericCell(formatoEntero));
        colDividendos.setCellFactory(column -> createRightAlignedNumericCell(formatoEntero));
        colGastos.setCellFactory(column -> createRightAlignedNumericCell(formatoEntero));
        colUtilRealizada.setCellFactory(column -> createColoredCell(formatoMoneda));
        colUtilNoRealizada.setCellFactory(column -> createColoredCell(formatoMoneda));

        colRentabilidad.setCellFactory(column -> new TableCell<>() {
            private final NumberFormat nf = new DecimalFormat("#,##0.00 %", new DecimalFormatSymbols(localeChile));
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(nf.format(item));
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        });
    }

    private void cargarResumenCompleto() {
        // ... (este método se mantiene sin cambios)
        Long empresaId = filtroGrupo.getEmpresaId();
        Long custodioId = filtroGrupo.getCustodioId();
        String cuenta = filtroGrupo.getCuenta();

        Task<List<ResumenInstrumentoDto>> task = new Task<>() {
            @Override
            protected List<ResumenInstrumentoDto> call() throws Exception {
                return resumenService.generarResumen(empresaId, custodioId, cuenta);
            }
        };

        progressIndicator.visibleProperty().bind(task.runningProperty());
        tablaResumen.disableProperty().bind(task.runningProperty()); // Buena práctica: deshabilitar la tabla mientras carga

        task.setOnSucceeded(e -> {
            listaCompletaResumen = task.getValue();
            filtroGrupo.setInstrumentosDisponibles(listaCompletaResumen);
            filtrarYActualizarTotales(); // Actualiza la tabla con todos los datos recibidos
        });

        task.setOnFailed(e -> {
            limpiarVista();
            if (e.getSource() != null && e.getSource().getException() != null) {
                e.getSource().getException().printStackTrace();
                // Opcional: Mostrar un diálogo de error al usuario
            }
        });
        
        new Thread(task).start();
    }
    
    // --- CAMBIO 3: NUEVO MÉTODO PARA LIMPIAR LA VISTA Y EVITAR REPETIR CÓDIGO ---
    private void limpiarVista() {
        tablaResumen.getItems().clear();
        listaCompletaResumen.clear();
        if (filtroGrupo.getCmbNemonico() != null) {
            filtroGrupo.getCmbNemonico().getItems().clear();
        }
        // Limpiar también los labels de totales
        lblTotalUtilidadRealizada.setText("0");
        lblTotalUtilidadNoRealizada.setText("0");
        lblTotalUtilidadRealizada.setTextFill(Color.BLACK);
        lblTotalUtilidadNoRealizada.setTextFill(Color.BLACK);
    }

    private void filtrarYActualizarTotales() {
        // ... (este método se mantiene sin cambios)
        Long instrumentoIdSeleccionado = filtroGrupo.getInstrumentoId();
        List<ResumenInstrumentoDto> listaParaMostrar;

        if (instrumentoIdSeleccionado == null) {
            listaParaMostrar = new ArrayList<>(listaCompletaResumen);
        } else {
            listaParaMostrar = listaCompletaResumen.stream()
                    .filter(dto -> dto.getInstrumentoId() != null && dto.getInstrumentoId().equals(instrumentoIdSeleccionado))
                    .collect(Collectors.toList());
        }

        tablaResumen.setItems(FXCollections.observableArrayList(listaParaMostrar));
        actualizarLabelsDeTotales(listaParaMostrar);
    }
    
    private void actualizarLabelsDeTotales(List<ResumenInstrumentoDto> items) {
        // ... (este método se mantiene sin cambios)
        List<ResumenInstrumentoDto> itemsReales = items.stream()
                .filter(dto -> !"TOTALES".equals(dto.getNemo()))
                .collect(Collectors.toList());

        BigDecimal totalRealizada = itemsReales.stream().map(ResumenInstrumentoDto::getUtilidadRealizada).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNoRealizada = itemsReales.stream().map(ResumenInstrumentoDto::getUtilidadNoRealizada).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        NumberFormat formatoEntero = new DecimalFormat("#,##0", new DecimalFormatSymbols(localeChile));

        lblTotalUtilidadRealizada.setText(formatoEntero.format(totalRealizada));
        lblTotalUtilidadNoRealizada.setText(formatoEntero.format(totalNoRealizada));

        lblTotalUtilidadRealizada.setTextFill(totalRealizada.compareTo(BigDecimal.ZERO) >= 0 ? Color.GREEN : Color.RED);
        lblTotalUtilidadNoRealizada.setTextFill(totalNoRealizada.compareTo(BigDecimal.ZERO) >= 0 ? Color.GREEN : Color.RED);
    }

    @Override
    public void setMainPane(BorderPane mainPane) {
        this.mainPane = mainPane;
    }

    // --- MÉTODOS HELPER (se mantienen sin cambios) ---
    private TableCell<ResumenInstrumentoDto, BigDecimal> createRightAlignedNumericCell(String formatPattern) {
        return new TableCell<>() {
            private final NumberFormat nf = new DecimalFormat(formatPattern, new DecimalFormatSymbols(localeChile));
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(nf.format(item));
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        };
    }

    private TableCell<ResumenInstrumentoDto, BigDecimal> createColoredCell(NumberFormat format) {
        return new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    // Aseguramos que el color vuelva a negro por defecto
                    setTextFill(Color.BLACK);
                } else {
                    setText(format.format(item));
                    setTextFill(item.compareTo(BigDecimal.ZERO) >= 0 ? Color.GREEN : Color.RED);
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        };
    }
}