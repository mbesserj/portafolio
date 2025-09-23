
package com.etl.interfaz;

import com.app.dto.CargaTransaccion;
import com.app.entities.CustodioEntity;
import com.app.entities.EmpresaEntity;
import com.app.entities.InstrumentoEntity;
import com.app.entities.ProductoEntity;
import com.app.entities.TipoMovimientoEntity;

public interface CargaProcessor {
    
    void process(CargaTransaccion dto,
                 EmpresaEntity empresa,
                 CustodioEntity custodio,
                 ProductoEntity producto,
                 InstrumentoEntity instrumento,
                 TipoMovimientoEntity tipoMovimiento);
}
