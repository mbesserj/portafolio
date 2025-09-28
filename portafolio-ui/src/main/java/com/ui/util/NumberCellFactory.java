package com.ui.util;

import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.paint.Color;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

public class NumberCellFactory {

    private static final Locale localeChile = new Locale("es", "CL");

    /**
     * Crea una celda para formatear un BigDecimal con un patrón y alineación derecha.
     */
    public static <T> TableCell<T, BigDecimal> forBigDecimal(String formatPattern) {
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

    /**
     * Crea una celda que además colorea el texto (rojo/verde) si es negativo/positivo.
     */
    public static <T> TableCell<T, BigDecimal> forBigDecimalWithColor(String formatPattern) {
        return new TableCell<>() {
            private final NumberFormat nf = new DecimalFormat(formatPattern, new DecimalFormatSymbols(localeChile));
            
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTextFill(Color.BLACK); // Reset color
                } else {
                    setText(nf.format(item));
                    setTextFill(item.compareTo(BigDecimal.ZERO) >= 0 ? Color.GREEN : Color.RED);
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        };
    }
}