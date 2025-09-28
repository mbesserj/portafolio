package com.ui.util;

import javafx.scene.control.TextFormatter;

import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * Un TextFormatter personalizado para JavaFX que solo permite la entrada de
 * números, incluyendo decimales (usando punto o coma) y un signo negativo opcional al inicio.
 */
public class NumberTextFormatter extends TextFormatter<String> {

    private static final Pattern VALID_NUMERIC_TEXT = Pattern.compile("-?(([1-9][0-9]*)|0)?([.,][0-9]*)?");

    public NumberTextFormatter() {
        super(createFilter());
    }

    private static UnaryOperator<Change> createFilter() {
        return change -> {
            String newText = change.getControlNewText();
            if (VALID_NUMERIC_TEXT.matcher(newText).matches()) {
                return change; 
            }
            return null; 
        };
    }
}
