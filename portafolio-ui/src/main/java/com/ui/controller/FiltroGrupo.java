package com.ui.controller;

import com.model.entities.CustodioEntity;
import com.model.entities.EmpresaEntity;
import com.model.entities.InstrumentoEntity;
import com.ui.factory.AppFacade;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.util.List;

import static com.ui.util.FormatUtils.createComboBoxCellFactory;

public class FiltroGrupo extends GridPane {

    @FXML
    protected ComboBox<EmpresaEntity> cmbEmpresa;
    @FXML
    protected ComboBox<CustodioEntity> cmbCustodio;
    @FXML
    protected ComboBox<String> cmbCuenta;
    @FXML
    protected ComboBox<InstrumentoEntity> cmbNemonico;

    private final ReadOnlyBooleanWrapper validSelection = new ReadOnlyBooleanWrapper(false);

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

    public void initializeComponent(AppFacade facade) {
        // Cargar empresas inicialmente
        facade.obtenerEmpresasConTransacciones()
                .ifSuccess(this::setEmpresas)
                .ifError(error -> System.err.println("Error cargando empresas: " + error));

        // Configurar listeners en cascada
        empresaProperty().addListener((obs, old, newVal) -> {
            limpiarCustodios();
            if (newVal != null) {
                facade.obtenerCustodiosConTransacciones(newVal.getId())
                        .ifSuccess(this::setCustodios)
                        .ifError(error -> System.err.println("Error cargando custodios: " + error));
            }
        });

        custodioProperty().addListener((obs, old, newVal) -> {
            limpiarCuentas();
            if (newVal != null && getEmpresaId() != null) {
                facade.obtenerCuentasConTransacciones(getEmpresaId(), newVal.getId())
                        .ifSuccess(this::setCuentas)
                        .ifError(error -> System.err.println("Error cargando cuentas: " + error));
            }
        });

        cuentaProperty().addListener((obs, old, newVal) -> {
            limpiarInstrumentos();
            if (newVal != null && !newVal.trim().isEmpty() && getEmpresaId() != null && getCustodioId() != null) {
                facade.obtenerInstrumentosConTransacciones(getEmpresaId(), getCustodioId(), newVal)
                        .ifSuccess(this::setInstrumentos)
                        .ifError(error -> System.err.println("Error cargando instrumentos: " + error));
            }
        });
    }

    @FXML
    private void initialize() {
        setupComboBoxes();

        // Será 'true' solo si todos los ComboBox tienen un valor seleccionado.
        BooleanBinding binding = cmbEmpresa.valueProperty().isNotNull()
                .and(cmbCustodio.valueProperty().isNotNull())
                .and(cmbCuenta.valueProperty().isNotNull())
                .and(cmbNemonico.valueProperty().isNotNull());

        // Conectamos nuestra propiedad a este vínculo.
        validSelection.bind(binding);
    }

    private void setupComboBoxes() {
        cmbEmpresa.setCellFactory(createComboBoxCellFactory(EmpresaEntity::getRazonSocial));
        cmbEmpresa.setButtonCell(createComboBoxCellFactory(EmpresaEntity::getRazonSocial).call(null));
        cmbCustodio.setCellFactory(createComboBoxCellFactory(CustodioEntity::getNombreCustodio));
        cmbCustodio.setButtonCell(createComboBoxCellFactory(CustodioEntity::getNombreCustodio).call(null));
        cmbNemonico.setCellFactory(createComboBoxCellFactory(InstrumentoEntity::getInstrumentoNombre));
        cmbNemonico.setButtonCell(createComboBoxCellFactory(InstrumentoEntity::getInstrumentoNemo).call(null));

        limpiarCustodios();
    }

    // --- MÉTODOS PARA POBLAR DATOS (Implementados) ---
    public void setEmpresas(List<EmpresaEntity> empresas) {
        Platform.runLater(() -> {
            cmbEmpresa.setItems(FXCollections.observableArrayList(empresas));
            if (empresas.size() == 1) {
                cmbEmpresa.setValue(empresas.get(0));
            }
        });
    }

    public void setCustodios(List<CustodioEntity> custodios) {
        Platform.runLater(() -> {
            cmbCustodio.setItems(FXCollections.observableArrayList(custodios));
            cmbCustodio.setDisable(custodios.isEmpty());
            if (custodios.size() == 1) {
                cmbCustodio.setValue(custodios.get(0));
            }
        });
    }

    public void setCuentas(List<String> cuentas) {
        Platform.runLater(() -> {
            cmbCuenta.setItems(FXCollections.observableArrayList(cuentas));
            cmbCuenta.setDisable(cuentas.isEmpty());
            if (cuentas.size() == 1) {
                cmbCuenta.setValue(cuentas.get(0));
            }
        });
    }

    public void setInstrumentos(List<InstrumentoEntity> instrumentos) {
        Platform.runLater(() -> {
            cmbNemonico.setItems(FXCollections.observableArrayList(instrumentos));
            cmbNemonico.setDisable(instrumentos.isEmpty());
            if (instrumentos.size() == 1) {
                cmbNemonico.setValue(instrumentos.get(0));
            }
        });
    }

    // --- MÉTODOS PARA LIMPIAR Y REINICIAR ESTADOS (Implementados) ---
    public void limpiarCustodios() {
        Platform.runLater(() -> {
            cmbCustodio.getItems().clear();
            cmbCustodio.setValue(null);
            cmbCustodio.setDisable(true);
            limpiarCuentas();
        });
    }

    public void limpiarCuentas() {
        Platform.runLater(() -> {
            cmbCuenta.getItems().clear();
            cmbCuenta.setValue(null);
            cmbCuenta.setDisable(true);
            limpiarInstrumentos();
        });
    }

    public void limpiarInstrumentos() {
        Platform.runLater(() -> {
            cmbNemonico.getItems().clear();
            cmbNemonico.setValue(null);
            cmbNemonico.setDisable(true);
        });
    }

    // --- GETTERS PARA COMBOBOXES (Para compatibilidad con otros controladores) ---
    public ComboBox<EmpresaEntity> getCmbEmpresa() {
        return cmbEmpresa;
    }

    public ComboBox<CustodioEntity> getCmbCustodio() {
        return cmbCustodio;
    }

    public ComboBox<String> getCmbCuenta() {
        return cmbCuenta;
    }

    // CORRECCIÓN: Este método debe retornar el tipo correcto
    public ComboBox<InstrumentoEntity> getCmbInstrumento() {
        return cmbNemonico;
    }

    // Método adicional para compatibilidad con ResumenHistoricoController
    public ComboBox<InstrumentoEntity> getCmbNemonico() {
        return cmbNemonico;
    }

    // --- GETTERS PARA IDs ---
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

    // --- PROPIEDADES PARA BINDING ---
    public ObjectProperty<EmpresaEntity> empresaProperty() {
        return cmbEmpresa.valueProperty();
    }

    public ObjectProperty<CustodioEntity> custodioProperty() {
        return cmbCustodio.valueProperty();
    }

    public ObjectProperty<String> cuentaProperty() {
        return cmbCuenta.valueProperty();
    }

    public ObjectProperty<InstrumentoEntity> nemoValueProperty() {
        return cmbNemonico.valueProperty();
    }

    // --- PROPIEDADES DE VALIDACIÓN ---
    /**
     * Expone la propiedad de solo lectura que indica si la selección del filtro
     * es válida. Ideal para hacer "bindings" desde otros controladores.
     *
     * @return Una ReadOnlyBooleanProperty.
     */
    public ReadOnlyBooleanProperty validSelectionProperty() {
        return validSelection.getReadOnlyProperty();
    }

    /**
     * Getter simple para comprobar el estado actual de la selección.
     *
     * @return true si la selección es válida, false en caso contrario.
     */
    public boolean isValidSelection() {
        return validSelection.get();
    }
}