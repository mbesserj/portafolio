package com.etl.loader;

import com.etl.interfaz.CargaMapperInterfaz;
import org.apache.poi.ss.usermodel.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public abstract class AbstractExcelMapper<T> implements CargaMapperInterfaz<T> {

    protected String getString(Cell cell) {
        if (cell == null) {
            return null;
        }
        return new DataFormatter().formatCellValue(cell).trim();
    }

    protected BigDecimal getBigDecimal(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            } else if (cell.getCellType() == CellType.STRING) {
                String value = cell.getStringCellValue().replace(".", "").replace(",", ".");
                if (value.matches("[-+]?\\d*\\.?\\d+")) {
                    return new BigDecimal(value);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    protected LocalDate getLocalDate(Cell cell) {
        if (cell == null) {
            return null;
        }

        // 1. Intenta leer la fecha si Excel la tiene en formato numérico (es más eficiente)
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
        } catch (Exception ignored) {
            // Si falla, se intentará leer como texto a continuación
        }

        // 2. Si no es una fecha numérica, la lee como texto
        String dateStr = getString(cell);
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }

        // 3. Define una lista con todos los formatos de fecha que quieres aceptar
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"), // Formato ISO (2025-08-26)
                DateTimeFormatter.ofPattern("dd/MM/yyyy"), // Formato con slash (26/08/2025)
                DateTimeFormatter.ofPattern("dd-MM-yyyy") // Formato con guion (26-08-2025)
        );

        // 4. Intenta convertir la fecha con cada formato de la lista
        for (DateTimeFormatter formatter : formatters) {
            try {
                // Si la conversión es exitosa, devuelve el resultado y termina el método
                return LocalDate.parse(dateStr, formatter);
            } catch (Exception ignored) {
                // Si este formato no funciona, el bucle continuará y probará el siguiente
            }
        }

        // 5. Si después de probar todos los formatos ninguno funcionó, devuelve null
        return null;
    }

    /**
     * Verifica si una fila debe ser ignorada (ej. está vacía).
     *
     * @param row La fila a verificar.
     * @return true si la fila debe ser omitida, false en caso contrario.
     */
    protected boolean shouldSkipRow(Row row) {
        // Por defecto, se basa en la primera celda.
        Cell firstCell = row.getCell(0);
        return firstCell == null || firstCell.getCellType() == CellType.BLANK || getString(firstCell).isBlank();
    }
}