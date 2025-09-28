package com.ui.controller;

import com.model.dto.ResumenInstrumentoDto;
import com.model.entities.InstrumentoEntity;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.scene.control.ListCell;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Componente de filtro especializado para vistas de portafolio.
 * Hereda la estructura de FiltroGrupo pero simplifica su lógica para ser
 * controlado externamente por un controlador padre.
 */
public class FiltroPortafolio extends FiltroGrupo {

    private static final InstrumentoEntity TODOS_INSTRUMENTOS = new InstrumentoEntity();

    static {
        TODOS_INSTRUMENTOS.setId(-1L);
        TODOS_INSTRUMENTOS.setInstrumentoNemo("(Todos)");
        TODOS_INSTRUMENTOS.setInstrumentoNombre("Mostrar todos los instrumentos");
    }

    private BooleanBinding validSelectionSinInstrumento;

    public FiltroPortafolio() {
        super(); 

        setupCustomCellFactories();

        // La lógica de validación ahora es local y simple.
        this.validSelectionSinInstrumento = cmbEmpresa.valueProperty().isNotNull()
                .and(cmbCustodio.valueProperty().isNotNull())
                .and(cmbCuenta.valueProperty().isNotNull());
    }

    private void setupCustomCellFactories() {
        // Configura cómo se muestran los ítems en el ComboBox de instrumentos.
        cmbNemonico.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(InstrumentoEntity item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // Muestra el nombre completo en la lista desplegable.
                    setText(item.getId().equals(-1L) ? item.getInstrumentoNemo() : item.getInstrumentoNombre());
                }
            }
        });
        cmbNemonico.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(InstrumentoEntity item, boolean empty) {
                super.updateItem(item, empty);
                // Muestra solo el Nemo en el ComboBox cuando está cerrado.
                setText(empty || item == null ? null : item.getInstrumentoNemo());
            }
        });
    }

    /**
     * Método público para que el controlador padre le pase la lista de instrumentos.
     * El componente ya no los busca, solo los recibe y los muestra.
     */
    public void setInstrumentosDisponibles(List<ResumenInstrumentoDto> resumenList) {
        List<InstrumentoEntity> instrumentosEnGrilla = resumenList.stream()
                .filter(dto -> dto.getInstrumentoId() != null)
                .map(dto -> {
                    InstrumentoEntity inst = new InstrumentoEntity();
                    inst.setId(dto.getInstrumentoId());
                    inst.setInstrumentoNemo(dto.getNemo());
                    inst.setInstrumentoNombre(dto.getNombreInstrumento());
                    return inst;
                })
                .collect(Collectors.toList());

        instrumentosEnGrilla.add(0, TODOS_INSTRUMENTOS);

        cmbNemonico.setItems(FXCollections.observableArrayList(instrumentosEnGrilla));
        cmbNemonico.getSelectionModel().select(TODOS_INSTRUMENTOS);
        cmbNemonico.setDisable(instrumentosEnGrilla.size() <= 1);
    }

    /**
     * Sobrescribe el método de la clase padre para una lógica específica.
     * Si no se ha seleccionado nada o se ha seleccionado "(Todos)", devuelve null.
     */
    @Override
    public Long getInstrumentoId() {
        InstrumentoEntity seleccionado = cmbNemonico.getValue();
        if (seleccionado == null || seleccionado.getId().equals(-1L)) {
            return null;
        }
        return seleccionado.getId();
    }

    public BooleanBinding validSelectionSinInstrumentoProperty() {
        return validSelectionSinInstrumento;
    }

}