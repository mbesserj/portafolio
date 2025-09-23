package com.etl.interfaz;

import com.app.exception.MappingException; 
import org.apache.poi.ss.usermodel.Row;

public interface CargaMapperInterfaz<T> {
    T map(Row row, int rowNum, String fileName) throws MappingException;
    
}