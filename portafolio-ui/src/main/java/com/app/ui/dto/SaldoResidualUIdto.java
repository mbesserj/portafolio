package com.app.ui.dto;

import com.app.dto.SaldoResidualDto;
import com.app.entities.InstrumentoEntity;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import java.math.BigDecimal;

public class SaldoResidualUIdto {

    private final BooleanProperty seleccionado = new SimpleBooleanProperty(false);
    private final SaldoResidualDto saldoOriginal;
    private final InstrumentoEntity instrumento;

    // El constructor ahora recibe el InstrumentoEntity ya cargado
    public SaldoResidualUIdto(SaldoResidualDto saldoOriginal, InstrumentoEntity instrumento) {
        this.saldoOriginal = saldoOriginal;
        this.instrumento = instrumento;
    }

    // --- Propiedades para la TableView ---
    public boolean isSeleccionado() { return seleccionado.get(); }
    public void setSeleccionado(boolean seleccionado) { this.seleccionado.set(seleccionado); }
    public BooleanProperty seleccionadoProperty() { return seleccionado; }

    // --- Getters para mostrar datos en las columnas ---
    public InstrumentoEntity getInstrumento() { return this.instrumento; }
    public BigDecimal getSaldoCantidad() { return saldoOriginal.getSaldoCantidad(); }
    public BigDecimal getSaldoValor() { return saldoOriginal.getSaldoValor(); }
    public SaldoResidualDto getSaldoOriginal() { return saldoOriginal; }
}