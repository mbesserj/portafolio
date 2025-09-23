package com.app.repository;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Contrato para operaciones de consulta sobre Precios.
 */
public interface PrecioRepository {
    Map<Long, BigDecimal> obtenerUltimosPreciosParaGrupo(Long empresaId, Long custodioId);
}
