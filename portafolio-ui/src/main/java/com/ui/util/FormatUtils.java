package com.ui.util;

import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.function.Function;

/**
 * Clase de utilidad para crear CellFactories personalizadas en JavaFX.
 * Contiene métodos estáticos para formatear celdas de tablas y ComboBox.
 */
public final class FormatUtils {
    
    private FormatUtils() {
    }

    /**
     * Crea un CellFactory para columnas de tabla que muestran valores numéricos (BigDecimal)
     * formateados según un patrón y alineados a la derecha.
     *
     * @param pattern El patrón de formato (ej. "#,##0.00" para dos decimales).
     * @param <S> El tipo del objeto de la fila de la tabla.
     * @return Un Callback que formatea la celda.
     */
    public static <S> Callback<TableColumn<S, BigDecimal>, TableCell<S, BigDecimal>> createNumericCellFactory(String pattern) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("es", "CL"));
        DecimalFormat format = new DecimalFormat(pattern, symbols);

        return column -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(format.format(item));
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        };
    }

    /**
     * Crea un CellFactory para ComboBox, permitiendo mostrar un texto personalizado
     * para cada objeto del ComboBox.
     *
     * @param textExtractor Una función que extrae el String a mostrar del objeto.
     * @param <T> El tipo del objeto en el ComboBox.
     * @return Un Callback que configura la celda del ComboBox.
     */
    public static <T> Callback<ListView<T>, ListCell<T>> createComboBoxCellFactory(Function<T, String> textExtractor) {
        return lv -> new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : textExtractor.apply(item));
            }
        };
    }
}