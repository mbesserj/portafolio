package com.app.repositorio;

import com.app.interfaces.AbstractRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import com.app.interfaces.PrecioRepInterfaz;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementación del Repositorio de Precios.
 * Contiene la lógica de acceso a datos (JPA, SQL) para los precios de mercado.
 */
public class PrecioRepositoryImpl extends AbstractRepository implements PrecioRepInterfaz {
    
    private static final Logger logger = LoggerFactory.getLogger(PrecioRepositoryImpl.class);

    private static final String ULTIMOS_PRECIOS_QUERY = """
        SELECT s.instrumento.id, s.precio FROM SaldoEntity s
        WHERE s.empresa.id = :empresaId AND s.custodio.id = :custodioId AND s.fecha = (
            SELECT MAX(sub.fecha) FROM SaldoEntity sub
            WHERE sub.empresa.id = :empresaId AND sub.custodio.id = :custodioId AND sub.fecha <= :fechaActual
        )
        """;

    @Override
    public Map<Long, BigDecimal> obtenerUltimosPreciosParaGrupo(Long empresaId, Long custodioId) {
        return executeReadOnly(em -> {
            try {
                return em.createQuery(ULTIMOS_PRECIOS_QUERY, Object[].class)
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