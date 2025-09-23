package com.etl.loader;

import com.app.dto.CartolaBanChile;
import com.app.exception.MappingException;
import com.app.utiles.Pk;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CartolaBanChileMapper extends AbstractExcelMapper<CartolaBanChile> {

    private final String hojaTipo;
    private LocalDate transactionDate;

    private static final Logger logger = LoggerFactory.getLogger(CartolaBanChileMapper.class);

    public CartolaBanChileMapper(String hojaTipo) {
        this.hojaTipo = hojaTipo;
    }

    @Override
    public CartolaBanChile map(Row row, int rowNum, String fileName) throws MappingException {

        if (shouldSkipRow(row)) {
            return null;
        }

        try {

            Cell fechaCell = row.getCell(1);
            if (fechaCell != null && fechaCell.getCellType() != CellType.BLANK) {

                // La forma más robusta de leer una fecha con Apache POI.
                // Esto funciona tanto si la celda está formateada como fecha
                // o si es un número (como 43831).
                LocalDateTime fechaLocalDateTime = fechaCell.getLocalDateTimeCellValue();

                // Convierte a LocalDate, que es lo que usualmente se guarda en la BD.
                transactionDate = fechaLocalDateTime.toLocalDate();

                try {
                    CartolaBanChile dto = new CartolaBanChile();
                    LocalDate fecha = hojaTipo.equals("S") ? getLocalDate(row.getCell(1))
                            : getLocalDate(row.getCell(3));

                    dto.setId(new Pk(fecha, rowNum, hojaTipo));

                    if ("S".equalsIgnoreCase(hojaTipo)) {
                        dto.setClienteSaldo(getString(row.getCell(0)));
                        dto.setFechaSaldo(transactionDate);
                        dto.setCuentaSaldo(getString(row.getCell(2)));
                        dto.setProductoSaldo(getString(row.getCell(3)));
                        dto.setInstrumentoSaldo(getString(row.getCell(4)));
                        dto.setNombreSaldo(getString(row.getCell(5)));
                        dto.setEmisor(getString(row.getCell(6)));
                        dto.setMonedaOrigenSaldo(getString(row.getCell(7)));
                        dto.setMontoInicialOrigen(getBigDecimal(row.getCell(8)));
                        dto.setIngresoNetoOrigen(getBigDecimal(row.getCell(9)));
                        dto.setMontoFinalOrigen(getBigDecimal(row.getCell(10)));
                        dto.setMontoFinalClp(getBigDecimal(row.getCell(11)));
                        dto.setNominalesFinal(getBigDecimal(row.getCell(12)));
                        dto.setPrecioTasaSaldo(getBigDecimal(row.getCell(13)));
                        dto.setVariacionPeriodoOrigen(getBigDecimal(row.getCell(14)));
                        dto.setRentabilidadPeriodoOrigen(getBigDecimal(row.getCell(15)));
                    } else {
                        dto.setClienteMovimiento(getString(row.getCell(0)));
                        dto.setCuentaMovimiento(getString(row.getCell(1)));
                        dto.setFechaLiquidacion(getLocalDate(row.getCell(2)));
                        dto.setFechaMovimiento(getLocalDate(row.getCell(3)));
                        dto.setProductoMovimiento(getString(row.getCell(4)));
                        dto.setMovimientoCaja(getString(row.getCell(5)));
                        dto.setOperacion(getString(row.getCell(6)));
                        dto.setInstrumentoNemo(getString(row.getCell(7)));
                        dto.setInstrumentoNombre(getString(row.getCell(8)));

                        dto.setCantidad(getBigDecimal(row.getCell(10)));
                        dto.setMonedaOrigen(getString(row.getCell(11)));
                        dto.setPrecio(getBigDecimal(row.getCell(12)));
                        dto.setComision(getBigDecimal(row.getCell(13)));
                        dto.setIva(getBigDecimal(row.getCell(14)));
                        dto.setMontoTransadoMO(getBigDecimal(row.getCell(15)));
                        dto.setMontoTransadoClp(getBigDecimal(row.getCell(16)));

                        String detalle = getString(row.getCell(9));
                        dto.setDetalle(detalle);

                        if ("A Caja".equalsIgnoreCase(detalle)) {
                            dto.getId().setTipoClase("C");
                        } else {
                            dto.getId().setTipoClase(hojaTipo);
                        }
                    }

                    if (dto.getInstrumentoNemo() == null || dto.getInstrumentoNemo().isBlank()) {
                        logger.warn("Se omitió la fila {} porque InstrumentoNemo está vacío.", rowNum);
                        return null;
                    }
                    return dto;

                } catch (Exception e) {
                    throw new MappingException("Error al mapear la fila " + rowNum + " del archivo " + fileName, e);
                }

            } else {
                // La celda de la fecha está vacía, lanza un error claro.
                logger.info("La fecha está vacía o es incorrecta.");
            }

        } catch (Exception e) {
            logger.error("Formato de fecha inválido en la fila. Error: " + e.getMessage());
        }
        return null;
    }
}
