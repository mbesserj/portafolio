package com.serv.repositorio;

import com.model.interfaces.AbstractRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.model.interfaces.PrecioRepository;
import com.serv.sql.QueryRepository;

/**
 * Implementación del Repositorio de Precios.
 * Contiene la lógica de acceso a datos (JPA, SQL) para los precios de mercado.
 */
public class PrecioRepositoryImpl extends AbstractRepository implements PrecioRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(PrecioRepositoryImpl.class);

    @Override
    public Map<Long, BigDecimal> obtenerUltimosPreciosParaGrupo(Long empresaId, Long custodioId) {
        return executeReadOnly(em -> {
            try {
                String sql = QueryRepository.getPreciosQuery(QueryRepository.PreciosQueries.ULTIMOS_PRECIOS_QUERY);
                return em.createQuery(sql, Object[].class)
                        .setParameter("empresaId", empresaId)
                        .setParameter("custodioId", custodioId)
                        .setParameter("fechaActual", LocalDate.now())
                        .getResultStream()
                        .collect(Collectors.toMap(
                            row -> (Long) row[0],
                            row -> (row[1] instanceof BigDecimal) ? (BigDecimal) row[1] : BigDecimal.ZERO,
                            (precioExistente, nuevoPrecio) -> precioExistente // En caso de duplicados
                        ));
            } catch (Exception e) {
                logger.error("Error al obtener mapa de precios para el grupo", e);
                return Collections.emptyMap();
            }
        });
    }
}