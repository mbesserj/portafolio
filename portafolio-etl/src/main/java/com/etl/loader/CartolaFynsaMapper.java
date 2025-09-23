package com.etl.loader;

import com.app.dto.CartolaFynsa;
import com.app.exception.MappingException;
import com.app.utiles.Pk;
import java.math.BigDecimal;
import org.apache.poi.ss.usermodel.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CartolaFynsaMapper extends AbstractExcelMapper<CartolaFynsa> {

    private final String tipoClase;
    private static final Logger logger = LoggerFactory.getLogger(CartolaFynsaMapper.class);

    public CartolaFynsaMapper(String tipoClase) {
        this.tipoClase = tipoClase;
    }

    @Override
    public CartolaFynsa map(Row row, int rowNum, String fileName) throws MappingException {

        if (shouldSkipRow(row)) {
            return null;
        }

        try {
            CartolaFynsa dto = new CartolaFynsa();
            dto.setTipoClase(tipoClase);
            dto.setRowNum(rowNum);

            if ("S".equalsIgnoreCase(tipoClase)) {
                dto.setTransactionDate(getLocalDate(row.getCell(0)));
                dto.setRazonSocial(getString(row.getCell(1)));
                dto.setRut(getString(row.getCell(2)));
                dto.setCuenta(getString(row.getCell(3)));
                dto.setCuentaPsh(getString(row.getCell(4)));
                dto.setCustodio(getString(row.getCell(5)));
                dto.setInstrumentoNemo(getString(row.getCell(6)));
                dto.setInstrumentoNombre(getString(row.getCell(7)));
                dto.setCantLibre(getBigDecimal(row.getCell(8)));
                dto.setCantGarantia(getBigDecimal(row.getCell(9)));
                dto.setCantPlazo(getBigDecimal(row.getCell(10)));
                dto.setCantVc(getBigDecimal(row.getCell(11)));
                dto.setCantTotal(getBigDecimal(row.getCell(12)));
                dto.setPrecio(getBigDecimal(row.getCell(13)));
                dto.setMontoClp(getBigDecimal(row.getCell(14)));
                dto.setMontoUsd(getBigDecimal(row.getCell(15)));
                dto.setMoneda(getString(row.getCell(16)));

            } else if ("T".equalsIgnoreCase(tipoClase)) {
                dto.setTransactionDate(getLocalDate(row.getCell(0)));
                dto.setRazonSocial(getString(row.getCell(1)));
                dto.setRut(getString(row.getCell(2)));
                dto.setCuenta(getString(row.getCell(3)));
                dto.setCustodio(getString(row.getCell(4)));
                dto.setTipoMovimiento(getString(row.getCell(5)));
                dto.setFolio(getString(row.getCell(6)));
                dto.setInstrumentoNemo(getString(row.getCell(7)));
                dto.setInstrumentoNombre(getString(row.getCell(8)));
                dto.setCantidad(getBigDecimal(row.getCell(9)));
                dto.setPrecio(getBigDecimal(row.getCell(10)));
                dto.setMonto(getBigDecimal(row.getCell(11)));
                dto.setComisiones(getBigDecimal(row.getCell(12)));
                dto.setGastos(getBigDecimal(row.getCell(13)));
                dto.setMontoTotal(getBigDecimal(row.getCell(14)));
                dto.setMoneda(getString(row.getCell(15)));

            } else if ("C".equalsIgnoreCase(tipoClase)) {

                String folio = getString(row.getCell(6));
                if (folio == null || "0".equals(folio)) {
                    return null;
                }

                dto.setTransactionDate(getLocalDate(row.getCell(0)));
                dto.setRazonSocial(getString(row.getCell(1)));
                dto.setRut(getString(row.getCell(2)));
                dto.setCuenta(getString(row.getCell(3)));
                dto.setCustodio(getString(row.getCell(4)));
                dto.setTipoMovimiento(getString(row.getCell(5)));
                dto.setFolio(folio);
                dto.setInstrumentoNemo(getString(row.getCell(7)));
                dto.setInstrumentoNombre(getString(row.getCell(8)));
                //dto.setCantidad(getBigDecimal(row.getCell(9)));
                dto.setPrecio(getBigDecimal(row.getCell(10)));
                dto.setMonto(getBigDecimal(row.getCell(11)));
                dto.setMoneda(getString(row.getCell(12)));
                dto.setComisiones(BigDecimal.ZERO);
                dto.setGastos(BigDecimal.ZERO);
                dto.setMontoTotal(dto.getMonto());
            }

            if (dto.getInstrumentoNemo() == null || dto.getInstrumentoNemo().isBlank()) {
                logger.warn("Se omitió la fila {} porque InstrumentoNemo está vacío.", rowNum);
                return null;
            }

            dto.setId(new Pk(dto.getTransactionDate(), rowNum, tipoClase));
            return dto;

        } catch (Exception e) {
            throw new MappingException("Error al mapear la fila " + rowNum + " del archivo " + fileName, e);
        }
    }
}
