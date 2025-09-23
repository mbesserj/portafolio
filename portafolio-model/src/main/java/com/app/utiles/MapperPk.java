package com.app.utiles;

import org.mapstruct.Mapper;
import java.time.LocalDate;

@Mapper(componentModel = "jakarta")
public interface MapperPk {

    default Pk map(LocalDate transactionDate, Integer rowNum, String tipoMovimiento) {
        if (transactionDate == null || rowNum == null || tipoMovimiento == null) {
            return null;
        }
        return new Pk(transactionDate, rowNum, tipoMovimiento);
    }
}