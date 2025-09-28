package com.ui.controller;

import com.model.dto.ResumenSaldoDto;
import com.model.entities.CustodioEntity;
import com.model.entities.EmpresaEntity;
import com.ui.factory.AppFacade;
import com.ui.factory.BaseController;
import com.ui.factory.ServiceResult;
import com.ui.util.Alertas;
import com.ui.util.MainPaneAware;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;

import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import static com.ui.util.FormatUtils.createComboBoxCellFactory;
import static com.ui.util.FormatUtils.createNumericCellFactory;

public class SaldosController extends BaseController implements MainPaneAware, Initializable {

    // --- Referencia al panel principal ---
    private BorderPane mainPane;
    private ResourceBundle resourceBundle;

    // --- Componentes FXML ---
    @FXML
    private TableView<ResumenSaldoDto> tablaSaldos;
    @FXML
    private TableColumn<ResumenSaldoDto, String> colNemo;
    @FXML
    private TableColumn<ResumenSaldoDto, String> colNombreInstrumento;
    @FXML
    private TableColumn<ResumenSaldoDto, BigDecimal> colPrecioMercado;
    @FXML
    private TableColumn<ResumenSaldoDto, BigDecimal> colSaldo;
    @FXML
    private TableColumn<ResumenSaldoDto, BigDecimal> colCostoTotal;
    @FXML
    private TableColumn<ResumenSaldoDto, BigDecimal> colCostoUnitario;
    @FXML
    private TableColumn<ResumenSaldoDto, BigDecimal> colValorMercado;
    @FXML
    private TableColumn<ResumenSaldoDto, BigDecimal> colUtilidad;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private ComboBox<EmpresaEntity> cmbEmpresa;
    @FXML
    private ComboBox<CustodioEntity> cmbCustodio;
    @FXML
    private Button btnBuscar;
    @FXML
    private Label lblTotalInversion;
    @FXML
    private Label lblTotalMercado;
    @FXML
    private Label lblTotalUtilidad;

    public SaldosController(AppFacade facade, ResourceBundle bundle) {
        super(facade, bundle);
    }

    @Override
    public void setMainPane(BorderPane mainPane) {
        this.mainPane = mainPane;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupFilters();
        btnBuscar.disableProperty().bind(
                cmbEmpresa.getSelectionModel().selectedItemProperty().isNull()
                        .or(cmbCustodio.getSelectionModel().selectedItemProperty().isNull())
        );
    }

    @FXML
    private void handleBuscarSaldos() {
        EmpresaEntity empresa = cmbEmpresa.getValue();
        CustodioEntity custodio = cmbCustodio.getValue();

        if (empresa == null || custodio == null) {
            Alertas.mostrarAlertaAdvertencia("Selección Incompleta", "Por favor, seleccione una empresa y un custodio.");
            return;
        }

        Task<List<ResumenSaldoDto>> loadTask = new Task<>() {
            @Override
            protected List<ResumenSaldoDto> call() throws Exception {
                ServiceResult<List<ResumenSaldoDto>> result
                        = facade.obtenerSaldosValorizados(empresa.getId(), custodio.getId());

                if (result.isSuccess()) {
                    return result.getData();
                } else {
                    throw new RuntimeException(result.getErrorMessage());
                }
            }
        };

        progressIndicator.visibleProperty().bind(loadTask.runningProperty());
        btnBuscar.disableProperty().bind(loadTask.runningProperty());

        loadTask.setOnSucceeded(e -> {
            cargarResultadosEnTabla(loadTask.getValue());
        });

        loadTask.setOnFailed(e -> {
            Alertas.mostrarAlertaError("Error de Carga", "Ocurrió un error al cargar los saldos.");
            loadTask.getException().printStackTrace();
            limpiarTotales();
        });

        new Thread(loadTask).start();
    }

    @FXML
    private void handleCerrar(ActionEvent event) {
        if (mainPane != null) {
            mainPane.setCenter(null);
        }
    }

    private void setupFilters() {
        ObservableList<EmpresaEntity> empresas = FXCollections.observableArrayList(facade.obtenerEmpresaTodas());
        cmbEmpresa.setItems(empresas);
        cmbEmpresa.setCellFactory(createComboBoxCellFactory(EmpresaEntity::getRazonSocial));
        cmbEmpresa.setButtonCell(createComboBoxCellFactory(EmpresaEntity::getRazonSocial).call(null));
        cmbEmpresa.valueProperty().addListener((obs, oldVal, newVal) -> actualizarFiltroCustodios(newVal));
    }

    private void actualizarFiltroCustodios(EmpresaEntity empresaSeleccionada) {
        cmbCustodio.getItems().clear();
        if (empresaSeleccionada != null) {
            List<CustodioEntity> custodios = facade.obtenerTodos();
            cmbCustodio.setItems(FXCollections.observableArrayList(custodios));
            cmbCustodio.setDisable(false);
        } else {
            cmbCustodio.setDisable(true);
        }
        cmbCustodio.setCellFactory(createComboBoxCellFactory(CustodioEntity::getNombreCustodio));
        cmbCustodio.setButtonCell(createComboBoxCellFactory(CustodioEntity::getNombreCustodio).call(null));
    }

    private void setupTableColumns() {
        colSaldo.setCellValueFactory(new PropertyValueFactory<>("saldoCantidad"));
        colCostoUnitario.setCellValueFactory(new PropertyValueFactory<>("costoUnitario"));
        colCostoTotal.setCellValueFactory(new PropertyValueFactory<>("costoTotal"));
        colValorMercado.setCellValueFactory(new PropertyValueFactory<>("valorMercado"));
        colUtilidad.setCellValueFactory(new PropertyValueFactory<>("utilidadNoRealizada"));
        colNombreInstrumento.setCellValueFactory(new PropertyValueFactory<>("instrumentoNombre"));
        colNemo.setCellValueFactory(new PropertyValueFactory<>("instrumentoNemo"));
        colPrecioMercado.setCellValueFactory(new PropertyValueFactory<>("precioMercado"));

        colSaldo.setCellFactory(createNumericCellFactory("#,##0.00"));
        colCostoUnitario.setCellFactory(createNumericCellFactory("#,##0.00"));
        colCostoTotal.setCellFactory(createNumericCellFactory("#,##0"));
        colValorMercado.setCellFactory(createNumericCellFactory("#,##0"));
        colUtilidad.setCellFactory(createNumericCellFactory("#,##0"));
        colPrecioMercado.setCellFactory(createNumericCellFactory("#,##0.00"));
    }

    private void cargarResultadosEnTabla(List<ResumenSaldoDto> resultados) {
        tablaSaldos.setItems(FXCollections.observableArrayList(resultados));
        actualizarTotales();
    }

    private void actualizarTotales() {
        BigDecimal totalInversion = tablaSaldos.getItems().stream()
                .map(ResumenSaldoDto::getCostoTotal)
                .filter(val -> val != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalMercado = tablaSaldos.getItems().stream()
                .map(ResumenSaldoDto::getValorMercado)
                .filter(val -> val != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalUtilidad = tablaSaldos.getItems().stream()
                .map(ResumenSaldoDto::getUtilidadNoRealizada)
                .filter(val -> val != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        lblTotalInversion.setText("$" + String.format("%,.2f", totalInversion));
        lblTotalMercado.setText("$" + String.format("%,.2f", totalMercado));
        lblTotalUtilidad.setText("$" + String.format("%,.2f", totalUtilidad));
    }

    private void limpiarTotales() {
        lblTotalInversion.setText("$0");
        lblTotalMercado.setText("$0");
        lblTotalUtilidad.setText("$0");
    }
}
