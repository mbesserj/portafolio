package com.etl.loader;

import com.app.dto.CartolaFynsa;
import com.etl.interfaz.AbstractCargaProcessor;
import com.etl.interfaz.AbstractCarga;
import com.etl.interfaz.CargaMapperInterfaz;
import jakarta.persistence.EntityManager;
import java.io.File;

public class CargaFynsaService extends AbstractCarga<CartolaFynsa> {

    /**
     * CONSTRUCTOR CORREGIDO: Ya no recibe directoryPath y solo llama a super(entityManager).
     */
    public CargaFynsaService(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    protected CargaMapperInterfaz<CartolaFynsa> getMapper(String sheetType) {
        return new CartolaFynsaMapper(sheetType);
    }

    @Override
    protected AbstractCargaProcessor<CartolaFynsa> getProcessor(String sheetType) {
        return new FynsaProcessor(entityManager);
    }

    @Override
    protected String getSheetType(File file, int sheetIndex) {
        String fileName = file.getName().toLowerCase();
        if (fileName.startsWith("stock") && sheetIndex == 0) return "S";
        if (fileName.startsWith("mvto")) {
            if (sheetIndex == 0) return "C";
            if (sheetIndex == 1) return "T";
        }
        return "";
    }
}