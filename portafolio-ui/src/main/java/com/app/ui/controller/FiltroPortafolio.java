package com.app.ui.controller;

import com.app.dto.ResumenInstrumentoDto;
import com.app.entities.InstrumentoEntity;
import com.app.interfaces.InstrumentoData;
import com.app.service.EmpresaService;
import com.app.service.FiltroService;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.scene.control.ListCell;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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
    }

    @Override
    public void initializeComponent(EmpresaService empresaService, FiltroService filtroService) {
        super.initializeComponent(empresaService, filtroService);
        this.validSelectionSinInstrumento = getCmbEmpresa().valueProperty().isNotNull()
                .and(getCmbCustodio().valueProperty().isNotNull())
                .and(getCmbCuenta().valueProperty().isNotNull());
    }

    @Override
    protected void setupComboBoxes() {
        super.setupComboBoxes();
        getCmbNemonico().setCellFactory(param -> new ListCell<InstrumentoEntity>() {
            @Override
            protected void updateItem(InstrumentoEntity item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item.getId().equals(-1L) ? item.getInstrumentoNemo() : item.getInstrumentoNombre()));
            }
        });
        getCmbNemonico().setButtonCell(new ListCell<InstrumentoEntity>() {
            @Override
            protected void updateItem(InstrumentoEntity item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getInstrumentoNemo());
            }
        });
    }

    /**
     * Nuevo método público para que el controlador pueble el ComboBox.
     */
    public void setInstrumentosDisponibles(List<ResumenInstrumentoDto> resumenList) {
        // 1. Filtramos la lista para quedarnos solo con los DTOs que son instrumentos reales.
        //    Ignoramos las filas especiales como "TOTALES" que no tienen un ID.
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

        // 2. Añadimos nuestra opción especial "(Todos)" al principio.
        instrumentosEnGrilla.add(0, TODOS_INSTRUMENTOS);

        // 3. Poblamos y preseleccionamos el ComboBox.
        getCmbNemonico().setItems(FXCollections.observableArrayList(instrumentosEnGrilla));
        getCmbNemonico().getSelectionModel().select(TODOS_INSTRUMENTOS);
        getCmbNemonico().setDisable(false);
    }

    public void setInstrumentosDisponibles(Collection<? extends InstrumentoData> itemsConInstrumento) {
        getCmbNemonico().getItems().clear(); // Limpia el combo

        if (itemsConInstrumento == null) {
            return;
        }

        List<InstrumentoEntity> instrumentosEnGrilla = itemsConInstrumento.stream()
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

        getCmbNemonico().setItems(FXCollections.observableArrayList(instrumentosEnGrilla));
        getCmbNemonico().getSelectionModel().select(TODOS_INSTRUMENTOS);
        getCmbNemonico().setDisable(false);
    }

    /**
     * Sobrescrito y vacío para que el controlador tome el control.
     */
    @Override
    protected void actualizarInstrumentos() {
        // No hacer nada.
    }

    @Override
    public Long getInstrumentoId() {
        InstrumentoEntity seleccionado = getCmbNemonico().getValue();
        if (seleccionado == null || seleccionado.getId().equals(-1L)) {
            return null;
        }
        return seleccionado.getId();
    }

    public BooleanBinding validSelectionSinInstrumentoProperty() {
        return validSelectionSinInstrumento;
    }
}
