package com.etl.loader;

import com.etl.interfaz.AbstractCargaProcessor;
import com.etl.interfaz.CargaMapperInterfaz;
import com.etl.interfaz.AbstractCarga;
import com.app.dto.CartolaBanChile;
import jakarta.persistence.EntityManager;
import java.io.File;

public class CargaBanChileService extends AbstractCarga<CartolaBanChile> {

    /**
     * CONSTRUCTOR CORREGIDO: Ya no recibe directoryPath y solo llama a super(entityManager).
     */
    public CargaBanChileService(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    protected CargaMapperInterfaz<CartolaBanChile> getMapper(String hojaTipo) {
        return new CartolaBanChileMapper(hojaTipo);
    }

    @Override
    protected AbstractCargaProcessor<CartolaBanChile> getProcessor(String hojaTipo) {
        return new BanChileProcessor(entityManager, hojaTipo);
    }

    @Override
    protected int getHeaderRowIndex() {
        return 4;
    }

    @Override
    protected String getSheetType(File file, int sheetIndex) {
        if (sheetIndex == 0) return "S";
        if (sheetIndex == 1) return "T";
        return "T";
    }
}