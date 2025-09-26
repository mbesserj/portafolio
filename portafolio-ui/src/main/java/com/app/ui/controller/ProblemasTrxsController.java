package com.app.ui.controller;

import com.app.dto.ProblemasTrxsDto;
import com.app.entities.CustodioEntity;
import com.app.entities.EmpresaEntity;
import com.app.service.CustodioService;
import com.app.service.EmpresaService;
import com.app.service.ProblemasTrxsService;
import com.app.ui.util.MainPaneAware;
import com.app.ui.util.ReportTask;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

import static com.app.ui.util.FormatUtils.createComboBoxCellFactory;
import static com.app.ui.util.FormatUtils.createNumericCellFactory;

public class ProblemasTrxsController implements MainPaneAware {

    private BorderPane mainPane;
    private final ProblemasTrxsService problemasService;
    private final EmpresaService empresaService;
    private final CustodioService custodioService;

    // --- Componentes FXML ---
    @FXML
    private ComboBox<EmpresaEntity> cmbEmpresa;
    @FXML
    private ComboBox<CustodioEntity> cmbCustodio;
    @FXML
    private Button btnBuscar;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private TableView<ProblemasTrxsDto> tablaTransacciones;
    @FXML
    private TableColumn<ProblemasTrxsDto, LocalDate> colFecha;
    @FXML
    private TableColumn<ProblemasTrxsDto, String> colFolio;
    @FXML
    private TableColumn<ProblemasTrxsDto, String> colTipo;
    @FXML
    private TableColumn<ProblemasTrxsDto, String> colNemo;
    @FXML
    private TableColumn<ProblemasTrxsDto, BigDecimal> colCompras;
    @FXML
    private TableColumn<ProblemasTrxsDto, BigDecimal> colVentas;
    @FXML
    private TableColumn<ProblemasTrxsDto, BigDecimal> colPrecio;
    @FXML
    private TableColumn<ProblemasTrxsDto, BigDecimal> colTotal;
    @FXML
    private TableColumn<ProblemasTrxsDto, Boolean> colCosteado;

    public ProblemasTrxsController(ProblemasTrxsService problemasService, EmpresaService empresaService, CustodioService custodioService) {
        this.problemasService = problemasService;
        this.empresaService = empresaService;
        this.custodioService = custodioService;
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        setupFilters();
        btnBuscar.disableProperty().bind(
            cmbEmpresa.getSelectionModel().selectedItemProperty().isNull()
            .or(cmbCustodio.getSelectionModel().selectedItemProperty().isNull())
            .or(progressIndicator.visibleProperty())
        );
    }

    private void setupTableColumns() {
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colFolio.setCellValueFactory(new PropertyValueFactory<>("folio"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipoMovimiento"));
        colNemo.setCellValueFactory(new PropertyValueFactory<>("instrumentoNemo"));
        colCompras.setCellValueFactory(new PropertyValueFactory<>("compras"));
        colVentas.setCellValueFactory(new PropertyValueFactory<>("ventas"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        
        colCosteado.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item ? "Sí" : "No"));
            }
        });

        colCompras.setCellFactory(createNumericCellFactory("#,##0.0"));
        colVentas.setCellFactory(createNumericCellFactory("#,##0.0"));
        colPrecio.setCellFactory(createNumericCellFactory("#,##0.00"));
        colTotal.setCellFactory(createNumericCellFactory("#,##0"));
    }

    private void setupFilters() {
        ObservableList<EmpresaEntity> empresas = FXCollections.observableArrayList(empresaService.obtenerTodas());
        ObservableList<CustodioEntity> custodios = FXCollections.observableArrayList(custodioService.obtenerTodos());

        cmbEmpresa.setItems(empresas);
        cmbCustodio.setItems(custodios);

        cmbEmpresa.setCellFactory(createComboBoxCellFactory(EmpresaEntity::getRazonSocial));
        cmbEmpresa.setButtonCell(createComboBoxCellFactory(EmpresaEntity::getRazonSocial).call(null));
        cmbCustodio.setCellFactory(createComboBoxCellFactory(CustodioEntity::getNombreCustodio));
        cmbCustodio.setButtonCell(createComboBoxCellFactory(CustodioEntity::getNombreCustodio).call(null));
    }

    @FXML
    private void handleBuscar() {
        EmpresaEntity empresa = cmbEmpresa.getValue();
        CustodioEntity custodio = cmbCustodio.getValue();

        if (empresa == null || custodio == null) {
            new Alert(Alert.AlertType.WARNING, "Debe seleccionar una Empresa y un Custodio para realizar la búsqueda.").showAndWait();
            return;
        }

        Supplier<List<ProblemasTrxsDto>> servicio = () -> problemasService.obtenerTransaccionesConProblemas(empresa.getRazonSocial(), custodio.getNombreCustodio());

        ReportTask<ProblemasTrxsDto> task = new ReportTask<>(tablaTransacciones, progressIndicator, null, servicio);
        new Thread(task).start();
    }

    @Override
    public void setMainPane(BorderPane mainPane) {
        this.mainPane = mainPane;
    }

    @FXML
    private void handleCerrar() {
        if (mainPane != null) {
            mainPane.setCenter(null);
        }
    }
}