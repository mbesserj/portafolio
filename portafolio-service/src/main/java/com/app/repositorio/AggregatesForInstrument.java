package com.app.repositorio;

import com.app.service.AbstractRepository;
import jakarta.persistence.Query;
import java.util.Optional;

public class AggregatesForInstrument extends AbstractRepository {

    private static final String AGGREGATES_QUERY = """
        SELECT
            SUM(CASE WHEN tm.tipo_movimiento LIKE '%Dividendo%' THEN t.monto_clp ELSE 0 END) as total_dividendos,
            SUM(COALESCE(t.gasto,0) + COALESCE(t.iva,0) + COALESCE(t.comision,0)) as total_gastos
        FROM transacciones t
        JOIN tipo_movimientos tm ON t.movimiento_id = tm.id
        WHERE t.empresa_id = :empresaId
          AND t.custodio_id = :custodioId
          AND t.cuenta = :cuenta
          AND t.instrumento_id = :instrumentoId
        """;

    public Optional<Object[]> getAggregatesForInstrument(Long empresaId, Long custodioId, String cuenta, Long instrumentoId) {
        return executeReadOnly(em -> {
            try {
                Query query = em.createNativeQuery(AGGREGATES_QUERY);
                query.setParameter("empresaId", empresaId);
                query.setParameter("custodioId", custodioId);
                query.setParameter("cuenta", cuenta);
                query.setParameter("instrumentoId", instrumentoId);

                return Optional.ofNullable((Object[]) query.getSingleResult());
            } catch (Exception e) {
                logger.error("Error al obtener agregados para instrumento", e);
                return Optional.empty();
            }
        });
    }
}
