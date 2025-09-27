package com.app.ui.controller;

import com.app.entities.CustodioEntity;
import com.app.entities.EmpresaEntity;
import com.app.entities.InstrumentoEntity;
import com.app.ui.factory.AppFacade;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.util.List;

import static com.app.ui.util.FormatUtils.createComboBoxCellFactory;

public class FiltroGrupo extends GridPane {

    @FXML protected ComboBox<EmpresaEntity> cmbEmpresa;
    @FXML protected ComboBox<CustodioEntity> cmbCustodio;
    @FXML protected ComboBox<String> cmbCuenta;
    @FXML protected ComboBox<InstrumentoEntity> cmbNemonico;

    // CAMBIO 1: La propiedad que comunicará el estado de validación.
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
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
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
    
    // --- MÉTODOS PARA POBLAR DATOS (Llamados por el controlador) ---
    public void setEmpresas(List<EmpresaEntity> empresas) { /* ... sin cambios ... */ }
    public void setCustodios(List<CustodioEntity> custodios) { /* ... sin cambios ... */ }
    public void setCuentas(List<String> cuentas) { /* ... sin cambios ... */ }
    public void setInstrumentos(List<InstrumentoEntity> instrumentos) { /* ... sin cambios ... */ }
    
    // --- MÉTODOS PARA LIMPIAR Y REINICIAR ESTADOS ---
    public void limpiarCustodios() { /* ... sin cambios ... */ }
    public void limpiarCuentas() { /* ... sin cambios ... */ }
    public void limpiarInstrumentos() { /* ... sin cambios ... */ }
    
    // --- GETTERS Y PROPIEDADES ---
    public Long getEmpresaId() { return cmbEmpresa.getValue() != null ? cmbEmpresa.getValue().getId() : null; }
    public Long getCustodioId() { return cmbCustodio.getValue() != null ? cmbCustodio.getValue().getId() : null; }
    public String getCuenta() { return cmbCuenta.getValue(); }
    public Long getInstrumentoId() { return cmbNemonico.getValue() != null ? cmbNemonico.getValue().getId() : null; }

    public ObjectProperty<EmpresaEntity> empresaProperty() { return cmbEmpresa.valueProperty(); }
    public ObjectProperty<CustodioEntity> custodioProperty() { return cmbCustodio.valueProperty(); }
    public ObjectProperty<String> cuentaProperty() { return cmbCuenta.valueProperty(); }
    public ObjectProperty<InstrumentoEntity> nemoValueProperty() { return cmbNemonico.valueProperty(); }

    // CAMBIO 3: Los métodos públicos para que otros controladores puedan "escuchar" la propiedad.
    /**
     * Expone la propiedad de solo lectura que indica si la selección del filtro es válida.
     * Ideal para hacer "bindings" desde otros controladores.
     * @return Una ReadOnlyBooleanProperty.
     */
    public ReadOnlyBooleanProperty validSelectionProperty() {
        return validSelection.getReadOnlyProperty();
    }

    /**
     * Getter simple para comprobar el estado actual de la selección.
     * @return true si la selección es válida, false en caso contrario.
     */
    public boolean isValidSelection() {
        return validSelection.get();
    }
}