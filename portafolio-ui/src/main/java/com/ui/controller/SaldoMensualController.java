package com.ui.controller;

import com.model.dto.SaldoMensualDto;
import com.model.entities.CustodioEntity;
import com.model.entities.EmpresaEntity;
import com.serv.service.CustodioService;
import com.serv.service.EmpresaService;
import com.serv.service.SaldoMensualService;
import com.ui.util.MainPaneAware;
import com.ui.util.ReportTask;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Year;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import static com.ui.util.FormatUtils.createComboBoxCellFactory;

public class SaldoMensualController implements MainPaneAware {

    private BorderPane mainPane;

    @FXML
    private ComboBox<EmpresaEntity> cmbEmpresa;
    @FXML
    private ComboBox<CustodioEntity> cmbCustodio;
    @FXML
    private Spinner<Integer> spinnerAnio;
    @FXML
    private ToggleGroup monedaToggleGroup;
    @FXML
    private RadioButton radioCLP;
    @FXML
    private RadioButton radioUSD;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private TableView<SaldoMensualDto> tablaSaldos;
    @FXML
    private TableColumn<SaldoMensualDto, String> colNemo;
    @FXML
    private TableColumn<SaldoMensualDto, String> colInstrumento;
    @FXML
    private TableColumn<SaldoMensualDto, BigDecimal> colEnero;
    @FXML
    private TableColumn<SaldoMensualDto, BigDecimal> colFebrero;
    @FXML
    private TableColumn<SaldoMensualDto, BigDecimal> colMarzo;
    @FXML
    private TableColumn<SaldoMensualDto, BigDecimal> colAbril;
    @FXML
    private TableColumn<SaldoMensualDto, BigDecimal> colMayo;
    @FXML
    private TableColumn<SaldoMensualDto, BigDecimal> colJunio;
    @FXML
    private TableColumn<SaldoMensualDto, BigDecimal> colJulio;
    @FXML
    private TableColumn<SaldoMensualDto, BigDecimal> colAgosto;
    @FXML
    private TableColumn<SaldoMensualDto, BigDecimal> colSeptiembre;
    @FXML
    private TableColumn<SaldoMensualDto, BigDecimal> colOctubre;
    @FXML
    private TableColumn<SaldoMensualDto, BigDecimal> colNoviembre;
    @FXML
    private TableColumn<SaldoMensualDto, BigDecimal> colDiciembre;

    private final SaldoMensualService saldoService;
    private final EmpresaService empresaService;
    private final CustodioService custodioService;

    private boolean isBuscando = false;

    public SaldoMensualController(SaldoMensualService saldoService, EmpresaService empresaService, CustodioService custodioService) {
        this.saldoService = saldoService;
        this.empresaService = empresaService;
        this.custodioService = custodioService;
    }

    @Override
    public void setMainPane(BorderPane mainPane) {
        this.mainPane = mainPane;
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        setupFiltersAndListeners();
    }

    private void setupTableColumns() {
        colNemo.setCellValueFactory(new PropertyValueFactory<>("nemo"));
        colInstrumento.setCellValueFactory(new PropertyValueFactory<>("instrumentoNemo"));
        colEnero.setCellValueFactory(new PropertyValueFactory<>("enero"));
        colFebrero.setCellValueFactory(new PropertyValueFactory<>("febrero"));
        colMarzo.setCellValueFactory(new PropertyValueFactory<>("marzo"));
        colAbril.setCellValueFactory(new PropertyValueFactory<>("abril"));
        colMayo.setCellValueFactory(new PropertyValueFactory<>("mayo"));
        colJunio.setCellValueFactory(new PropertyValueFactory<>("junio"));
        colJulio.setCellValueFactory(new PropertyValueFactory<>("julio"));
        colAgosto.setCellValueFactory(new PropertyValueFactory<>("agosto"));
        colSeptiembre.setCellValueFactory(new PropertyValueFactory<>("septiembre"));
        colOctubre.setCellValueFactory(new PropertyValueFactory<>("octubre"));
        colNoviembre.setCellValueFactory(new PropertyValueFactory<>("noviembre"));
        colDiciembre.setCellValueFactory(new PropertyValueFactory<>("diciembre"));

        formatNumericColumn(colEnero);
        formatNumericColumn(colFebrero);
        formatNumericColumn(colMarzo);
        formatNumericColumn(colAbril);
        formatNumericColumn(colMayo);
        formatNumericColumn(colJunio);
        formatNumericColumn(colJulio);
        formatNumericColumn(colAgosto);
        formatNumericColumn(colSeptiembre);
        formatNumericColumn(colOctubre);
        formatNumericColumn(colNoviembre);
        formatNumericColumn(colDiciembre);

        // lista de filas en negrita.
        final List<String> filasEnNegrita = Arrays.asList("Total general", "Utilidad/(Pérdida) Acum.");

        tablaSaldos.setRowFactory(tv -> new TableRow<SaldoMensualDto>() {
            @Override
            protected void updateItem(SaldoMensualDto item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else {
                    if (filasEnNegrita.contains(item.getNemo())) {
                        setStyle("-fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    private void setupFiltersAndListeners() {
        // Carga de Empresas
        cmbEmpresa.setItems(FXCollections.observableArrayList(empresaService.obtenerTodas()));
        cmbEmpresa.setCellFactory(createComboBoxCellFactory(EmpresaEntity::getRazonSocial));
        cmbEmpresa.setButtonCell(createComboBoxCellFactory(EmpresaEntity::getRazonSocial).call(null));

        // Carga de Custodios con opción "Todos"
        List<CustodioEntity> custodios = custodioService.obtenerTodos();
        CustodioEntity todosLosCustodios = new CustodioEntity();
        todosLosCustodios.setId(0L);
        todosLosCustodios.setNombreCustodio("Todos los custodios");
        custodios.add(0, todosLosCustodios);
        cmbCustodio.setItems(FXCollections.observableArrayList(custodios));
        cmbCustodio.setCellFactory(createComboBoxCellFactory(CustodioEntity::getNombreCustodio));
        cmbCustodio.setButtonCell(createComboBoxCellFactory(CustodioEntity::getNombreCustodio).call(null));

        // Configuración del año
        int currentYear = Year.now().getValue();
        spinnerAnio.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2000, currentYear + 5, currentYear));

        // Añadir listeners a todos los filtros para disparar la búsqueda
        cmbEmpresa.valueProperty().addListener((obs, oldVal, newVal) -> ejecutarBusqueda());
        cmbCustodio.valueProperty().addListener((obs, oldVal, newVal) -> ejecutarBusqueda());
        spinnerAnio.valueProperty().addListener((obs, oldVal, newVal) -> ejecutarBusqueda());
        monedaToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> ejecutarBusqueda());

        // Carga inicial de datos al abrir la ventana
        Platform.runLater(this::ejecutarBusqueda);
    }

    private void ejecutarBusqueda() {
        if (isBuscando) {
            return;
        }

        EmpresaEntity empresa = cmbEmpresa.getValue();
        CustodioEntity custodioSeleccionado = cmbCustodio.getValue();
        Integer anio = spinnerAnio.getValue();
        String moneda = radioUSD.isSelected() ? "USD" : "CLP";

        if (empresa == null || custodioSeleccionado == null || anio == null) {
            tablaSaldos.getItems().clear();
            return;
        }

        String nombreCustodio = custodioSeleccionado.getId() == 0L ? null : custodioSeleccionado.getNombreCustodio();

        // La llamada a "existenDatosParaFiltros" ha sido eliminada.
        Supplier<List<SaldoMensualDto>> servicio = ()
                -> saldoService.obtenerSaldosMensuales(empresa.getRazonSocial(), nombreCustodio, anio, moneda);

        ReportTask<SaldoMensualDto> task = new ReportTask<>(tablaSaldos, progressIndicator, null, servicio);

        task.setOnRunning(e -> isBuscando = true);
        task.setOnSucceeded(e -> isBuscando = false);
        task.setOnFailed(e -> isBuscando = false);

        new Thread(task).start();
    }

    @FXML
    private void handleCerrar(ActionEvent event) {
        if (mainPane != null) {
            mainPane.setCenter(null);
        }
    }

    private void formatNumericColumn(TableColumn<SaldoMensualDto, BigDecimal> column) {
        final Locale localeChile = new Locale("es", "CL");
        final NumberFormat integerFormat = NumberFormat.getNumberInstance(localeChile);
        integerFormat.setMaximumFractionDigits(0);
        final NumberFormat decimalFormat = NumberFormat.getNumberInstance(localeChile);
        decimalFormat.setMinimumFractionDigits(1);
        decimalFormat.setMaximumFractionDigits(1);

        column.setCellFactory(col -> new TableCell<SaldoMensualDto, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    SaldoMensualDto dto = (SaldoMensualDto) getTableRow().getItem();

                    // Comprueba si el texto de la columna Nemo EMPIEZA CON "Variación".
                    if (dto != null && dto.getNemo() != null && dto.getNemo().startsWith("Variación")) {
                        setText(decimalFormat.format(item) + "%");
                    } else {
                        setText(integerFormat.format(item));
                    }

                    // La lógica para el color rojo no cambia, pero también la ajustamos para ser flexible
                    String style = "-fx-alignment: CENTER-RIGHT; -fx-text-fill: black;";

                    if (dto != null && dto.getNemo() != null
                            && (dto.getNemo().startsWith("Variación") || "Utilidad/(Pérdida)".equals(dto.getNemo()))
                            && item.signum() == -1) {

                        style = "-fx-alignment: CENTER-RIGHT; -fx-text-fill: red;";
                    }

                    setStyle(style);
                }
            }
        });
    }
}
