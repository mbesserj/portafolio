package com.ui.controller;

import com.model.dto.ResumenHistoricoDto;
import com.model.dto.ResumenInstrumentoDto;
import com.serv.factory.ServiceContainer;
import com.serv.service.ResumenHistoricoService;
import com.ui.factory.AppFacade;
import com.ui.factory.BaseController;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

public class ResumenHistoricoController extends BaseController {

    private ResumenHistoricoService resumenService;
    private ResourceBundle resourceBundle;
    private final Locale localeChile = new Locale("es", "CL");
    private List<ResumenHistoricoDto> listaCompletaResumen = new ArrayList<>();

    @FXML
    private FiltroPortafolio filtroGrupo;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private TableView<ResumenHistoricoDto> tableViewResultados;
    @FXML
    private Label lblTotalUtilidad;

    @FXML
    private TableColumn<ResumenHistoricoDto, String> colNombreInstrumento;
    @FXML
    private TableColumn<ResumenHistoricoDto, BigDecimal> colCostoFifo;
    @FXML
    private TableColumn<ResumenHistoricoDto, BigDecimal> colGasto;
    @FXML
    private TableColumn<ResumenHistoricoDto, BigDecimal> colDividendo;
    @FXML
    private TableColumn<ResumenHistoricoDto, BigDecimal> colUtilidad;
    @FXML
    private TableColumn<ResumenHistoricoDto, BigDecimal> colTotal;

    public ResumenHistoricoController(AppFacade facade, ResourceBundle bundle) {
        super(facade, bundle);

        try {
            ServiceContainer container = ServiceContainer.getInstance();
            this.resumenService = container.getService(ResumenHistoricoService.class);
        } catch (Exception e) {
            logger.error("Error al obtener ResumenHistoricoService", e);
        }
    }

    @FXML
    public void initialize() {
        filtroGrupo.initializeComponent(facade);
        setupTableColumns();

        // OPCIÓN 1: Si FiltroPortafolio tiene getters tipados (RECOMENDADO)
        if (filtroGrupo.getCmbEmpresa() != null) {
            filtroGrupo.getCmbEmpresa().valueProperty().addListener((obs, oldVal, newVal) -> onFiltroPrincipalChanged());
        }
        if (filtroGrupo.getCmbCustodio() != null) {
            filtroGrupo.getCmbCustodio().valueProperty().addListener((obs, oldVal, newVal) -> onFiltroPrincipalChanged());
        }
        if (filtroGrupo.getCmbCuenta() != null) {
            filtroGrupo.getCmbCuenta().valueProperty().addListener((obs, oldVal, newVal) -> onFiltroPrincipalChanged());
        }

        // Para el nemo, usar el ComboBox de instrumento si existe
        if (filtroGrupo.getCmbInstrumento() != null) {
            filtroGrupo.getCmbInstrumento().valueProperty().addListener((obs, oldVal, newVal) -> {
                if (filtroGrupo.getCmbEmpresa() != null && filtroGrupo.getCmbEmpresa().getValue() != null) {
                    filtrarYActualizarTablaYTotales();
                }
            });
        }

        // OPCIÓN 2: Si FiltroPortafolio no tiene métodos públicos (ALTERNATIVA)
        // Usar reflexión o métodos directos del filtroGrupo
        tableViewResultados.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(ResumenHistoricoDto item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("fila-totales");
                if (item != null && "TOTALES".equals(item.getNemo())) {
                    getStyleClass().add("fila-totales");
                }
            }
        });
    }

    /**
     * Se llama cada vez que un filtro principal cambia. Decide si ejecutar la
     * consulta o limpiar la vista.
     */
    private void onFiltroPrincipalChanged() {
        if (filtroGrupo.validSelectionSinInstrumentoProperty().get()) {
            cargarResumenHistoricoCompleto();
        } else {
            limpiarVista();
        }
    }

    private void setupTableColumns() {
        colNombreInstrumento.setCellValueFactory(new PropertyValueFactory<>("nombreInstrumento"));
        // CORRECCIÓN 1: El nombre de la propiedad debe coincidir con el campo del DTO.
        colCostoFifo.setCellValueFactory(new PropertyValueFactory<>("totalCostoFifo"));
        colGasto.setCellValueFactory(new PropertyValueFactory<>("totalGasto"));
        colDividendo.setCellValueFactory(new PropertyValueFactory<>("totalDividendo"));
        colUtilidad.setCellValueFactory(new PropertyValueFactory<>("totalUtilidad"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalTotal"));

        String formatoEntero = "#,##0";
        NumberFormat formatoMoneda = new DecimalFormat(formatoEntero, new DecimalFormatSymbols(localeChile));

        colCostoFifo.setCellFactory(column -> createRightAlignedNumericCell(formatoEntero));
        colGasto.setCellFactory(column -> createRightAlignedNumericCell(formatoEntero));
        colDividendo.setCellFactory(column -> createRightAlignedNumericCell(formatoEntero));
        colUtilidad.setCellFactory(column -> createColoredCell(formatoMoneda));
        colTotal.setCellFactory(column -> createColoredCell(formatoMoneda));
    }

    private void cargarResumenHistoricoCompleto() {
        Long empresaId = filtroGrupo.getEmpresaId();
        Long custodioId = filtroGrupo.getCustodioId();
        String cuenta = filtroGrupo.getCuenta();

        Task<List<ResumenHistoricoDto>> task = new Task<>() {
            @Override
            protected List<ResumenHistoricoDto> call() throws Exception {
                return resumenService.generarReporte(empresaId, custodioId, cuenta);
            }
        };

        progressIndicator.visibleProperty().bind(task.runningProperty());
        tableViewResultados.disableProperty().bind(task.runningProperty());

        List<ResumenInstrumentoDto> instrumentos = listaCompletaResumen.stream()
                .filter(dto -> !"TOTALES".equals(dto.getNemo()))
                .map(dto -> {
                    ResumenInstrumentoDto instrumento = new ResumenInstrumentoDto();
                    instrumento.setInstrumentoId(dto.getInstrumentoId());
                    instrumento.setNemo(dto.getNemo());
                    instrumento.setNombreInstrumento(dto.getNombreInstrumento());
                    return instrumento;
                })
                .collect(Collectors.toList());
        filtroGrupo.setInstrumentosDisponibles(instrumentos);

        task.setOnFailed(e -> {
            limpiarVista();
            task.getException().printStackTrace();
        });
        new Thread(task).start();
    }

    private void filtrarYActualizarTablaYTotales() {
        Long instrumentoIdSeleccionado = filtroGrupo.getInstrumentoId();
        List<ResumenHistoricoDto> listaFiltrada;

        if (instrumentoIdSeleccionado == null) {
            listaFiltrada = new ArrayList<>(listaCompletaResumen);
        } else {
            List<ResumenHistoricoDto> datos = listaCompletaResumen.stream()
                    .filter(dto -> !"TOTALES".equals(dto.getNemo()))
                    .filter(dto -> Objects.equals(dto.getInstrumentoId(), instrumentoIdSeleccionado))
                    .collect(Collectors.toList());
            listaFiltrada = new ArrayList<>(datos);
        }

        tableViewResultados.setItems(FXCollections.observableArrayList(listaFiltrada));
        actualizarLabelsDeTotales(listaFiltrada);
    }

    private void actualizarLabelsDeTotales(List<ResumenHistoricoDto> items) {
        ResumenHistoricoDto totales = items.stream()
                .filter(dto -> "TOTALES".equals(dto.getNemo()))
                .findFirst()
                .orElse(null);

        BigDecimal totalFinal;

        if (totales != null) {
            totalFinal = totales.getTotalTotal();
        } else {
            totalFinal = items.stream()
                    .map(ResumenHistoricoDto::getTotalTotal)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        if (totalFinal == null) {
            totalFinal = BigDecimal.ZERO;
        }

        NumberFormat formatoEntero = new DecimalFormat("#,##0", new DecimalFormatSymbols(localeChile));
        lblTotalUtilidad.setText(formatoEntero.format(totalFinal));
        lblTotalUtilidad.setTextFill(totalFinal.compareTo(BigDecimal.ZERO) >= 0 ? Color.GREEN : Color.RED);
    }

    private void limpiarVista() {
        tableViewResultados.getItems().clear();
        listaCompletaResumen.clear();
        if (filtroGrupo.cmbNemonico.getItems() != null) {
            filtroGrupo.cmbNemonico.getItems().clear();
        }
        if (lblTotalUtilidad != null) {
            lblTotalUtilidad.setText("0");
            lblTotalUtilidad.setTextFill(Color.BLACK); // Resetear color
        }
    }

    // --- Métodos Helper ---
    private <T> TableCell<T, BigDecimal> createRightAlignedNumericCell(String formatPattern) {
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

    private <T> TableCell<T, BigDecimal> createColoredCell(NumberFormat format) {
        return new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
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
