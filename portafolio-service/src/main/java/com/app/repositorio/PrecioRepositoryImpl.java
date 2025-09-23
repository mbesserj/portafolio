package com.app.repositorio;

import com.app.repository.PrecioRepository;
import com.app.utiles.LibraryInitializer;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementación del Repositorio de Precios.
 * Contiene la lógica de acceso a datos (JPA, SQL) para los precios de mercado.
 */
public class PrecioRepositoryImpl implements PrecioRepository {
    private static final Logger logger = LoggerFactory.getLogger(PrecioRepositoryImpl.class);

    @Override
    public Map<Long, BigDecimal> obtenerUltimosPreciosParaGrupo(Long empresaId, Long custodioId) {
        EntityManager em = null;
        try {
            em = LibraryInitializer.getEntityManager();
            String jpql = """
                SELECT s.instrumento.id, s.precio FROM SaldoEntity s
                WHERE s.empresa.id = :empresaId AND s.custodio.id = :custodioId AND s.fecha = (
                    SELECT MAX(sub.fecha) FROM SaldoEntity sub
                    WHERE sub.empresa.id = :empresaId AND sub.custodio.id = :custodioId AND sub.fecha <= :fechaActual
                )
            """;
            return em.createQuery(jpql, Object[].class)
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
            logger.error("Error al obtener mapa de precios para el grupo.", e);
            return Collections.emptyMap();
        } finally {
            if (em != null) em.close();
        }
    }
}

