package com.app.repository;

import com.app.dto.ResultadoInstrumentoDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ResultadoInstrumentoRepository {

    /**
     * Obtiene las operaciones de compra/venta y calcula el saldo acumulado.
     * @param empresaId
     * @param custodioId
     * @param cuenta
     * @param instrumentoId
     * @return 
     */
    List<ResultadoInstrumentoDto> findOperacionesByFiltro(Long empresaId, Long custodioId, String cuenta, Long instrumentoId);

    /**
     * Obtiene únicamente los movimientos de dividendos.
     * @param empresaId
     * @param custodioId
     * @param cuenta
     * @param instrumentoId
     * @return 
     */
    List<ResultadoInstrumentoDto> findDividendosByFiltro(Long empresaId, Long custodioId, String cuenta, Long instrumentoId);
    
    /**
     * Obtiene los gastos asociados a cada transacción en un mapa para fácil acceso.
     * @param empresaId
     * @param custodioId
     * @param cuenta
     * @param instrumentoId
     * @return 
     */
    Map<Long, BigDecimal> findGastosByFiltro(Long empresaId, Long custodioId, String cuenta, Long instrumentoId);
}