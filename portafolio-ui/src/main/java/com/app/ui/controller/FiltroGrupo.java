package com.app.ui.controller;

import com.app.entities.CustodioEntity;
import com.app.entities.EmpresaEntity;
import com.app.entities.InstrumentoEntity;
import com.app.service.EmpresaService;
import com.app.service.FiltroService;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.util.List;

import static com.app.ui.util.FormatUtils.createComboBoxCellFactory;

public class FiltroGrupo extends GridPane {

    // --- CAMBIO: de private a protected para que las clases hijas puedan acceder ---
    @FXML
    protected ComboBox<EmpresaEntity> cmbEmpresa;
    @FXML
    protected ComboBox<CustodioEntity> cmbCustodio;
    @FXML
    protected ComboBox<String> cmbCuenta;
    @FXML
    protected ComboBox<InstrumentoEntity> cmbNemonico;

    protected EmpresaService empresaService;
    protected FiltroService filtroService;
    protected BooleanBinding validSelection;

    public FiltroGrupo() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/FiltroGrupo.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void initializeComponent(EmpresaService empresaService, FiltroService filtroService) {
        this.empresaService = empresaService;
        this.filtroService = filtroService;
        setupComboBoxes();
        this.validSelection = cmbEmpresa.valueProperty().isNotNull()
                .and(cmbCustodio.valueProperty().isNotNull())
                .and(cmbCuenta.valueProperty().isNotNull())
                .and(cmbNemonico.valueProperty().isNotNull());
    }

    protected void setupComboBoxes() {
        cmbEmpresa.setCellFactory(createComboBoxCellFactory(EmpresaEntity::getRazonSocial));
        cmbEmpresa.setButtonCell(createComboBoxCellFactory(EmpresaEntity::getRazonSocial).call(null));
        cmbCustodio.setCellFactory(createComboBoxCellFactory(CustodioEntity::getNombreCustodio));
        cmbCustodio.setButtonCell(createComboBoxCellFactory(CustodioEntity::getNombreCustodio).call(null));
        cmbNemonico.setCellFactory(createComboBoxCellFactory(instrumento -> instrumento.getInstrumentoNombre()));
        cmbNemonico.setButtonCell(createComboBoxCellFactory(InstrumentoEntity::getInstrumentoNemo).call(null));

        cmbEmpresa.setItems(FXCollections.observableArrayList(empresaService.obtenerTodas()));
        cmbCustodio.setDisable(true);
        cmbCuenta.setDisable(true);
        cmbNemonico.setDisable(true);

        cmbEmpresa.valueProperty().addListener((obs, oldVal, newVal) -> actualizarCustodios());
        cmbCustodio.valueProperty().addListener((obs, oldVal, newVal) -> actualizarCuentas());
        cmbCuenta.valueProperty().addListener((obs, oldVal, newVal) -> actualizarInstrumentos());
    }

    protected void actualizarCustodios() {
        cmbCustodio.getItems().clear();
        cmbCuenta.getItems().clear();
        cmbNemonico.getItems().clear();
        cmbCuenta.setDisable(true);
        cmbNemonico.setDisable(true);
        EmpresaEntity empresa = cmbEmpresa.getValue();
        if (empresa != null) {
            List<CustodioEntity> custodios = filtroService.obtenerCustodiosConTransacciones(empresa.getId());
            cmbCustodio.setItems(FXCollections.observableArrayList(custodios));
            cmbCustodio.setDisable(false);
        }
    }

    protected void actualizarCuentas() {
        cmbCuenta.getItems().clear();
        cmbNemonico.getItems().clear();
        cmbNemonico.setDisable(true);
        EmpresaEntity empresa = cmbEmpresa.getValue();
        CustodioEntity custodio = cmbCustodio.getValue();
        if (empresa != null && custodio != null) {
            List<String> cuentas = filtroService.obtenerCuentasConTransacciones(empresa.getId(), custodio.getId());
            cmbCuenta.setItems(FXCollections.observableArrayList(cuentas));
            cmbCuenta.setDisable(false);
        }
    }

    protected void actualizarInstrumentos() {
        cmbNemonico.getItems().clear();
        EmpresaEntity empresa = cmbEmpresa.getValue();
        CustodioEntity custodio = cmbCustodio.getValue();
        String cuenta = cmbCuenta.getValue();
        if (empresa != null && custodio != null && cuenta != null && !cuenta.isBlank()) {
            List<InstrumentoEntity> instrumentos = filtroService.obtenerInstrumentosConTransacciones(empresa.getId(), custodio.getId(), cuenta);
            cmbNemonico.setItems(FXCollections.observableArrayList(instrumentos));
            cmbNemonico.setDisable(false);
        }
    }

    // --- Métodos Públicos (sin cambios) ---
    public Long getEmpresaId() {
        return cmbEmpresa.getValue() != null ? cmbEmpresa.getValue().getId() : null;
    }

    public Long getCustodioId() {
        return cmbCustodio.getValue() != null ? cmbCustodio.getValue().getId() : null;
    }

    public String getCuenta() {
        return cmbCuenta.getValue();
    }

    public Long getInstrumentoId() {
        return cmbNemonico.getValue() != null ? cmbNemonico.getValue().getId() : null;
    }

    public BooleanBinding validSelectionProperty() {
        return validSelection;
    }

    public ObjectProperty<InstrumentoEntity> nemoValueProperty() {
        return cmbNemonico.valueProperty();
    }

    public ObjectProperty<EmpresaEntity> empresaProperty() {
        return cmbEmpresa.valueProperty();
    }

    public ObjectProperty<CustodioEntity> custodioProperty() {
        return cmbCustodio.valueProperty();
    }

    public ObjectProperty<String> cuentaProperty() {
        return cmbCuenta.valueProperty();
    }

    // --- NUEVOS GETTERS PROTEGIDOS PARA LAS CLASES HIJAS ---
    protected EmpresaEntity getEmpresa() {
        return cmbEmpresa.getValue();
    }

    protected CustodioEntity getCustodio() {
        return cmbCustodio.getValue();
    }

    protected ComboBox<InstrumentoEntity> getCmbNemonico() {
        return cmbNemonico;
    }

    protected FiltroService getFiltroService() {
        return filtroService;
    }

    protected ComboBox<EmpresaEntity> getCmbEmpresa() {
        return cmbEmpresa;
    }

    protected ComboBox<CustodioEntity> getCmbCustodio() {
        return cmbCustodio;
    }

    protected ComboBox<String> getCmbCuenta() {
        return cmbCuenta;
    }
}
