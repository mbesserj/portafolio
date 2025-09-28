package com.ui.controller;

import com.ui.util.TaskManager;
import com.model.dto.ResumenInstrumentoDto;
import com.model.entities.CustodioEntity;
import com.model.entities.EmpresaEntity;
import com.ui.factory.AppFacade;
import com.ui.factory.BaseController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Controlador para la vista de resumen de portafolio. Maneja la interfaz de
 * usuario para mostrar el resumen de inversiones por cuenta.
 */
public class ResumenPortafolioController extends BaseController implements Initializable {

    // ===== COMPONENTES FXML =====
    @FXML private ComboBox<EmpresaEntity> comboEmpresas;
    @FXML private ComboBox<CustodioEntity> comboCustodios;
    @FXML private ComboBox<String> comboCuentas;
    @FXML private Button btnCargarResumen;
    @FXML private Button btnRefrescar;
    @FXML private Button btnExportar;

    // Indicadores de estado
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label lblStatus;
    @FXML private Label lblUltimaActualizacion;

    // Tabla principal
    @FXML private TableView<ResumenInstrumentoDto> tableResumen;

    // Columnas de la tabla
    @FXML private TableColumn<ResumenInstrumentoDto, String> colNemo;
    @FXML private TableColumn<ResumenInstrumentoDto, String> colNombre;
    @FXML private TableColumn<ResumenInstrumentoDto, BigDecimal> colSaldoDisponible;
    @FXML private TableColumn<ResumenInstrumentoDto, BigDecimal> colCostoFifo;
    @FXML private TableColumn<ResumenInstrumentoDto, BigDecimal> colValorMercado;
    @FXML private TableColumn<ResumenInstrumentoDto, BigDecimal> colUtilidadRealizada;
    @FXML private TableColumn<ResumenInstrumentoDto, BigDecimal> colUtilidadNoRealizada;
    @FXML private TableColumn<ResumenInstrumentoDto, BigDecimal> colTotalDividendos;
    @FXML private TableColumn<ResumenInstrumentoDto, BigDecimal> colTotalGastos;
    @FXML private TableColumn<ResumenInstrumentoDto, BigDecimal> colRentabilidad;

    // Labels de totales
    @FXML private Label lblTotalCosto;
    @FXML private Label lblTotalValorMercado;
    @FXML private Label lblTotalUtilidad;
    @FXML private Label lblTotalRentabilidad;

    // ===== DATOS Y SERVICIOS =====

    private final ObservableList<ResumenInstrumentoDto> resumenData = FXCollections.observableArrayList();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CL"));
    private final NumberFormat percentFormat = NumberFormat.getPercentInstance(new Locale("es", "CL"));
    private ResourceBundle resourceBundle;
    
    public ResumenPortafolioController(AppFacade facade, ResourceBundle bundle) {
        super(facade, bundle);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupComboBoxes();
        setupEventListeners();
        loadInitialData();

        // Configurar formato de números
        percentFormat.setMinimumFractionDigits(2);
        percentFormat.setMaximumFractionDigits(2);
    }

    /**
     * Configura las columnas de la tabla con sus respectivos formatos
     */
    private void setupTableColumns() {
        // Configurar factory de valores
        colNemo.setCellValueFactory(new PropertyValueFactory<>("nemo"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreInstrumento"));
        colSaldoDisponible.setCellValueFactory(new PropertyValueFactory<>("saldoDisponible"));
        colCostoFifo.setCellValueFactory(new PropertyValueFactory<>("costoFifo"));
        colValorMercado.setCellValueFactory(new PropertyValueFactory<>("valorDeMercado"));
        colUtilidadRealizada.setCellValueFactory(new PropertyValueFactory<>("utilidadRealizada"));
        colUtilidadNoRealizada.setCellValueFactory(new PropertyValueFactory<>("utilidadNoRealizada"));
        colTotalDividendos.setCellValueFactory(new PropertyValueFactory<>("totalDividendos"));
        colTotalGastos.setCellValueFactory(new PropertyValueFactory<>("totalGastos"));
        colRentabilidad.setCellValueFactory(new PropertyValueFactory<>("rentabilidad"));

        // Configurar formatos de celdas para monedas
        setupCurrencyColumn(colCostoFifo);
        setupCurrencyColumn(colValorMercado);
        setupCurrencyColumn(colUtilidadRealizada);
        setupCurrencyColumn(colUtilidadNoRealizada);
        setupCurrencyColumn(colTotalDividendos);
        setupCurrencyColumn(colTotalGastos);

        // Configurar formato de porcentaje para rentabilidad
        colRentabilidad.setCellFactory(column -> new TableCell<ResumenInstrumentoDto, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(percentFormat.format(item.doubleValue()));

                    // Colorear según el valor
                    if (item.compareTo(BigDecimal.ZERO) > 0) {
                        setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    } else if (item.compareTo(BigDecimal.ZERO) < 0) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: black;");
                    }
                }
            }
        });

        // Configurar formato para cantidad (sin decimales)
        colSaldoDisponible.setCellFactory(column -> new TableCell<ResumenInstrumentoDto, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(NumberFormat.getIntegerInstance().format(item.longValue()));
                }
            }
        });

        // Configurar estilo especial para filas de totales
        tableResumen.setRowFactory(tv -> new TableRow<ResumenInstrumentoDto>() {
            @Override
            protected void updateItem(ResumenInstrumentoDto item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && "TOTALES".equals(item.getNemo())) {
                    setStyle("-fx-background-color: #e8f4fd; -fx-font-weight: bold;");
                } else {
                    setStyle("");
                }
            }
        });

        tableResumen.setItems(resumenData);
    }

    /**
     * Configura una columna para mostrar valores de moneda
     */
    private void setupCurrencyColumn(TableColumn<ResumenInstrumentoDto, BigDecimal> column) {
        column.setCellFactory(col -> new TableCell<ResumenInstrumentoDto, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(currencyFormat.format(item.doubleValue()));

                    // Colorear utilidades
                    if (col == colUtilidadRealizada || col == colUtilidadNoRealizada) {
                        if (item.compareTo(BigDecimal.ZERO) > 0) {
                            setStyle("-fx-text-fill: green;");
                        } else if (item.compareTo(BigDecimal.ZERO) < 0) {
                            setStyle("-fx-text-fill: red;");
                        } else {
                            setStyle("-fx-text-fill: black;");
                        }
                    }
                }
            }
        });
    }

    /**
     * Configura los ComboBox con sus conversores y listeners
     */
    private void setupComboBoxes() {
        // Configurar converter para empresas
        comboEmpresas.setConverter(new StringConverter<EmpresaEntity>() {
            @Override
            public String toString(EmpresaEntity empresa) {
                return empresa != null ? empresa.getRazonSocial() : "";
            }

            @Override
            public EmpresaEntity fromString(String string) {
                return null; // No necesario para ComboBox de solo lectura
            }
        });

        // Configurar converter para custodios
        comboCustodios.setConverter(new StringConverter<CustodioEntity>() {
            @Override
            public String toString(CustodioEntity custodio) {
                return custodio != null ? custodio.getNombreCustodio() : "";
            }

            @Override
            public CustodioEntity fromString(String string) {
                return null;
            }
        });

        // Configurar listeners para cascada
        comboEmpresas.setOnAction(e -> loadCustodios());
        comboCustodios.setOnAction(e -> loadCuentas());
        comboCuentas.setOnAction(e -> updateButtonStates());
    }

    /**
     * Configura los event listeners para botones
     */
    private void setupEventListeners() {
        btnCargarResumen.setOnAction(e -> cargarResumen());
        btnRefrescar.setOnAction(e -> refrescarDatos());
        btnExportar.setOnAction(e -> exportarDatos());

        // Doble click en tabla para ver detalle
        tableResumen.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tableResumen.getSelectionModel().getSelectedItem() != null) {
                verDetalleInstrumento();
            }
        });
    }

    /**
     * Carga los datos iniciales al abrir la pantalla
     */
    private void loadInitialData() {
        setLoadingState(true, "Cargando empresas...");

        TaskManager.executeAsync(
                () -> facade.obtenerEmpresasConTransacciones(),
                this::onEmpresasLoaded,
                this::handleError,
                () -> setLoadingState(false, "Listo")
        );
    }

    /**
     * Callback cuando se cargan las empresas exitosamente
     */
    private void onEmpresasLoaded(List<EmpresaEntity> empresas) {
        Platform.runLater(() -> {
            comboEmpresas.setItems(FXCollections.observableArrayList(empresas));
            if (!empresas.isEmpty()) {
                comboEmpresas.getSelectionModel().selectFirst();
                loadCustodios();
            }
            updateStatus("Empresas cargadas: " + empresas.size());
        });
    }

    /**
     * Carga los custodios para la empresa seleccionada
     */
    private void loadCustodios() {
        EmpresaEntity empresaSeleccionada = comboEmpresas.getSelectionModel().getSelectedItem();
        if (empresaSeleccionada == null) {
            comboCustodios.getItems().clear();
            comboCuentas.getItems().clear();
            return;
        }

        setLoadingState(true, "Cargando custodios...");

        TaskManager.executeAsync(
                () -> facade.obtenerCustodiosPorEmpresa(empresaSeleccionada.getId()),
                custodios -> Platform.runLater(() -> {
                    comboCustodios.setItems(FXCollections.observableArrayList(custodios));
                    comboCuentas.getItems().clear();
                    if (!custodios.isEmpty()) {
                        comboCustodios.getSelectionModel().selectFirst();
                        loadCuentas();
                    }
                    updateStatus("Custodios cargados: " + custodios.size());
                }),
                this::handleError,
                () -> setLoadingState(false, "")
        );
    }

    /**
     * Carga las cuentas para la empresa y custodio seleccionados
     */
    private void loadCuentas() {
        EmpresaEntity empresa = comboEmpresas.getSelectionModel().getSelectedItem();
        CustodioEntity custodio = comboCustodios.getSelectionModel().getSelectedItem();

        if (empresa == null || custodio == null) {
            comboCuentas.getItems().clear();
            return;
        }

        setLoadingState(true, "Cargando cuentas...");

        TaskManager.executeAsync(
                () -> facade.obtenerCuentasPorCustodioYEmpresa(custodio.getId(), empresa.getId()),
                cuentas -> Platform.runLater(() -> {
                    comboCuentas.setItems(FXCollections.observableArrayList(cuentas));
                    if (!cuentas.isEmpty()) {
                        comboCuentas.getSelectionModel().selectFirst();
                    }
                    updateButtonStates();
                    updateStatus("Cuentas cargadas: " + cuentas.size());
                }),
                this::handleError,
                () -> setLoadingState(false, "")
        );
    }

    /**
     * Actualiza el estado de los botones según la selección
     */
    private void updateButtonStates() {
        boolean seleccionCompleta = comboEmpresas.getSelectionModel().getSelectedItem() != null
                && comboCustodios.getSelectionModel().getSelectedItem() != null
                && comboCuentas.getSelectionModel().getSelectedItem() != null;

        btnCargarResumen.setDisable(!seleccionCompleta);
        btnRefrescar.setDisable(!seleccionCompleta || resumenData.isEmpty());
        btnExportar.setDisable(resumenData.isEmpty());
    }

    /**
     * Carga el resumen de portafolio
     */
    @FXML
    private void cargarResumen() {
        EmpresaEntity empresa = comboEmpresas.getSelectionModel().getSelectedItem();
        CustodioEntity custodio = comboCustodios.getSelectionModel().getSelectedItem();
        String cuenta = comboCuentas.getSelectionModel().getSelectedItem();

        if (empresa == null || custodio == null || cuenta == null) {
            showAlert(Alert.AlertType.WARNING, "Selección incompleta",
                    "Debe seleccionar empresa, custodio y cuenta antes de continuar.");
            return;
        }

        setLoadingState(true, "Generando resumen de portafolio...");

        TaskManager.executeAsync(
                () -> facade.obtenerResumenPortafolio(empresa.getId(), custodio.getId(), cuenta),
                this::onResumenLoaded,
                this::handleError,
                () -> setLoadingState(false, "")
        );
    }

    /**
     * Refresca los datos actuales
     */
    @FXML
    private void refrescarDatos() {
        cargarResumen();
    }

    /**
     * Exporta los datos a Excel/CSV
     */
    @FXML
    private void exportarDatos() {
        // TODO: Implementar exportación
        showAlert(Alert.AlertType.INFORMATION, "Exportar",
                "Funcionalidad de exportación en desarrollo");
    }

    /**
     * Callback cuando se carga el resumen exitosamente
     */
    private void onResumenLoaded(List<ResumenInstrumentoDto> resumen) {
        Platform.runLater(() -> {
            resumenData.clear();
            resumenData.addAll(resumen);
            updateTotalsLabels(resumen);
            updateButtonStates();
            updateLastUpdateTime();
            updateStatus("Resumen cargado: " + resumen.size() + " instrumentos");
        });
    }

    /**
     * Actualiza los labels de totales en la parte inferior
     */
    private void updateTotalsLabels(List<ResumenInstrumentoDto> resumen) {
        // Buscar la fila de totales
        ResumenInstrumentoDto totales = resumen.stream()
                .filter(dto -> "TOTALES".equals(dto.getNemo()))
                .findFirst()
                .orElse(null);

        if (totales != null) {
            lblTotalCosto.setText(currencyFormat.format(
                    totales.getCostoFifo() != null ? totales.getCostoFifo().doubleValue() : 0.0));
            lblTotalValorMercado.setText(currencyFormat.format(
                    totales.getValorDeMercado() != null ? totales.getValorDeMercado().doubleValue() : 0.0));

            BigDecimal utilidadTotal = BigDecimal.ZERO;
            if (totales.getUtilidadRealizada() != null) {
                utilidadTotal = utilidadTotal.add(totales.getUtilidadRealizada());
            }
            if (totales.getUtilidadNoRealizada() != null) {
                utilidadTotal = utilidadTotal.add(totales.getUtilidadNoRealizada());
            }

            lblTotalUtilidad.setText(currencyFormat.format(utilidadTotal.doubleValue()));

            if (totales.getRentabilidad() != null) {
                lblTotalRentabilidad.setText(percentFormat.format(totales.getRentabilidad().doubleValue()));

                // Colorear según el valor
                if (totales.getRentabilidad().compareTo(BigDecimal.ZERO) > 0) {
                    lblTotalRentabilidad.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                } else if (totales.getRentabilidad().compareTo(BigDecimal.ZERO) < 0) {
                    lblTotalRentabilidad.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                } else {
                    lblTotalRentabilidad.setStyle("-fx-text-fill: black;");
                }
            }
        }
    }

    /**
     * Muestra el detalle del instrumento seleccionado
     */
    private void verDetalleInstrumento() {
        ResumenInstrumentoDto selected = tableResumen.getSelectionModel().getSelectedItem();
        if (selected == null || "TOTALES".equals(selected.getNemo())) {
            return;
        }

        // TODO: Abrir ventana de detalle del instrumento
        showAlert(Alert.AlertType.INFORMATION, "Detalle",
                "Detalle de " + selected.getNemo() + " - " + selected.getNombreInstrumento());
    }

    /**
     * Maneja errores de manera consistente
     */
    private void handleError(Throwable error) {
        Platform.runLater(() -> {
            String message = error.getMessage() != null ? error.getMessage() : "Error desconocido";
            updateStatus("Error: " + message);
            showAlert(Alert.AlertType.ERROR, "Error", "Error en la operación:\n" + message);
        });
    }

    /**
     * Actualiza el estado de carga de la interfaz
     */
    private void setLoadingState(boolean loading, String message) {
        Platform.runLater(() -> {
            progressIndicator.setVisible(loading);
            if (message != null && !message.isEmpty()) {
                updateStatus(message);
            }

            // Deshabilitar controles durante la carga
            comboEmpresas.setDisable(loading);
            comboCustodios.setDisable(loading);
            comboCuentas.setDisable(loading);
            btnCargarResumen.setDisable(loading);
            btnRefrescar.setDisable(loading);
        });
    }

    /**
     * Actualiza el texto de estado
     */
    private void updateStatus(String status) {
        Platform.runLater(() -> lblStatus.setText(status));
    }

    /**
     * Actualiza la hora de última actualización
     */
    private void updateLastUpdateTime() {
        Platform.runLater(() -> {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            lblUltimaActualizacion.setText("Última actualización: " + timestamp);
        });
    }

    /**
     * Muestra un alert modal
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // ===== MÉTODOS PÚBLICOS PARA INTEGRACIÓN =====
    
    /**
     * Permite establecer la selección inicial desde otro controlador
     */
    public void setSeleccionInicial(Long empresaId, Long custodioId, String cuenta) {
        Platform.runLater(() -> {
            // Buscar y seleccionar empresa
            if (empresaId != null) {
                comboEmpresas.getItems().stream()
                        .filter(e -> e.getId().equals(empresaId))
                        .findFirst()
                        .ifPresent(empresa -> comboEmpresas.getSelectionModel().select(empresa));
            }

            // Buscar y seleccionar custodio (después de cargar)
            if (custodioId != null) {
                comboCustodios.getItems().stream()
                        .filter(c -> c.getId().equals(custodioId))
                        .findFirst()
                        .ifPresent(custodio -> comboCustodios.getSelectionModel().select(custodio));
            }

            // Buscar y seleccionar cuenta (después de cargar)
            if (cuenta != null) {
                comboCuentas.getItems().stream()
                        .filter(c -> c.equals(cuenta))
                        .findFirst()
                        .ifPresent(cta -> comboCuentas.getSelectionModel().select(cta));
            }
        });
    }

    /**
     * Limpia todos los datos y selecciones
     */
    public void limpiarDatos() {
        Platform.runLater(() -> {
            resumenData.clear();
            comboEmpresas.getSelectionModel().clearSelection();
            comboCustodios.getItems().clear();
            comboCuentas.getItems().clear();
            updateButtonStates();
            updateStatus("Datos limpiados");
        });
    }
}
